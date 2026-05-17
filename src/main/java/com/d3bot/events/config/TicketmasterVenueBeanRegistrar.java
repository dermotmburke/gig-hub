package com.d3bot.events.config;

import com.d3bot.events.pipelines.TicketmasterVenueEventPipeline;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Registers one {@link TicketmasterVenueEventPipeline} bean per entry in
 * {@code fetchers.ticketmaster.venues}. Runs before any beans are instantiated so the
 * dynamically created pipelines appear in the {@code List<EventPipeline>} injected
 * by {@link com.d3bot.events.runners.EventPipelineRunner}.
 *
 * <p>If {@code fetchers.ticketmaster.api-key} is absent no beans are registered.
 */
@Component
public class TicketmasterVenueBeanRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String apiKey = environment.getProperty("fetchers.ticketmaster.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        Set<String> venueNames = Binder.get(environment)
                .bind("fetchers.ticketmaster.venues", Bindable.mapOf(String.class, Object.class))
                .map(Map::keySet)
                .orElse(Set.of());

        for (String venueName : venueNames) {
            String venueId = environment.getProperty("fetchers.ticketmaster.venues." + venueName + ".id");
            if (venueId == null || venueId.isBlank()) {
                continue;
            }

            GenericBeanDefinition def = new GenericBeanDefinition();
            def.setBeanClass(TicketmasterVenueEventPipeline.class);
            def.setFactoryBeanName("ticketmasterPipelineFactory");
            def.setFactoryMethodName("create");
            def.getConstructorArgumentValues().addGenericArgumentValue(venueName);
            def.getConstructorArgumentValues().addGenericArgumentValue(venueId);
            def.getConstructorArgumentValues().addGenericArgumentValue(apiKey);

            registry.registerBeanDefinition(venueName + "TicketmasterEventPipeline", def);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
