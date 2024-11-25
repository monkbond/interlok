/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.core;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.LifecycleHelper;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Configurable implementation of <code>EventHandler</code> that allows the user to
 * send events to different producers based on rules. Rules are composed of an event matcher
 * which checks whether an event should be sent by it's standalone producer. If there are
 * no matching rules, the default configured producer is used.
 * </p>
 *
 * @config default-event-handler
 *
 */
@XStreamAlias("configurable-event-handler")
@AdapterComponent
@ComponentProfile(summary = "Sends MessageLifecycleEvents to a destination based on the matching rule, or default if no match", tag = "base,events")
public class ConfigurableEventHandler extends DefaultEventHandler {

  @Valid
  @NotNull
  @AutoPopulated
  @XStreamImplicit
  @Getter
  private List<Rule> rules;

  public ConfigurableEventHandler() {
    this(new NullConnection(), new NullMessageProducer());
  }

  public ConfigurableEventHandler(AdaptrisMessageProducer producer) {
    this(new NullConnection(), producer);
  }

  public ConfigurableEventHandler(AdaptrisConnection connection, AdaptrisMessageProducer producer)  {
    super(connection, producer);
    rules = new LinkedList<>();
  }

  /**
   * Determines the correct producer to use to send the event
   * @param event
   * @return the matched producer, or the default if no matches
   */
  protected AdaptrisMessageSender resolveEventSender(Event event) {
    for (Rule rule : rules) {
      if (rule.matcher.matches(event)) {
        return rule.standaloneProducer;
      }
    }
    return getProducer();
  }

  @Override
  public void send(Event evt, Map<String, String> properties) throws CoreException {
    AdaptrisMessageSender sender = resolveEventSender(evt);
    eventProducerDelegate.produce(sender, createMessage(evt, properties));
  }

  /** @see AdaptrisComponent#init() */
  @Override
  protected void eventHandlerInit() throws CoreException {
    super.eventHandlerInit();
    for(Rule rule : rules) {
      rule.standaloneProducer.init();
    }
  }

  /** @see AdaptrisComponent#start() */
  @Override
  protected void eventHandlerStart() throws CoreException {
    super.eventHandlerStart();
    for(Rule rule : rules) {
      rule.standaloneProducer.start();
    }
  }

  /** @see AdaptrisComponent#stop() */
  @Override
  protected void eventHandlerStop() {
    super.eventHandlerStop();
    for(Rule rule : rules) {
      rule.standaloneProducer.stop();
    }
  }

  /** @see AdaptrisComponent#close() */
  @Override
  protected void eventHandlerClose() {
    super.eventHandlerClose();
    for(Rule rule : rules) {
      rule.standaloneProducer.close();
    }
  }

  @Override
  public void prepare() throws CoreException {
    super.prepare();
    for(Rule rule : rules) {
      rule.standaloneProducer.prepare();
    }
  }


  /**
   * Matches on an event
   */
  interface EventMatcher {
    enum MatchType {
      SOURCE_ID(event -> event.getSourceId()),
      DESTINATION_ID(event -> event.getDestinationId()),
      NAMESPACE(event -> event.getNameSpace()),
      TYPE(event -> event.getClass().getCanonicalName());

      Function<Event, Object> getter;
      MatchType(Function<Event, Object> getter) {
        this.getter = getter;
      }

      public Object getProperty(Event event) {
        return getter.apply(event);
      }
    }

    boolean matches(Event event);
  }

  @XStreamAlias("regex-event-matcher")
  static class RegexEventMatcher implements EventMatcher {
    @NotNull
    private String regex;
    @NotNull
    private Set<MatchType> matchTypes;

    private transient  Pattern compiledRegex;

    public RegexEventMatcher(String regex, Set<MatchType> matchTypes) {
      this.regex = regex;
      this.matchTypes = matchTypes;
      this.compiledRegex = Pattern.compile(regex);
    }

    @Override
    public boolean matches(Event event) {
      for (MatchType matchType : matchTypes) {
        if (compiledRegex.matcher((String)matchType.getProperty(event)).matches()) return true;
      }
      return false;
    }
  }

  @XStreamAlias("rule")
  @AllArgsConstructor
  static class Rule {
    private EventMatcher matcher;
    private StandaloneProducer standaloneProducer;
  }
}
