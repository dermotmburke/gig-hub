package com.d3bot.events.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketmasterVenueBeanRegistrarTest {

    private final TicketmasterVenueBeanRegistrar registrar = new TicketmasterVenueBeanRegistrar();
    private final BeanDefinitionRegistry registry = mock(BeanDefinitionRegistry.class);

    @Test
    void registersBeanForEachConfiguredVenue() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("fetchers.ticketmaster.api-key", "test-key")
                .withProperty("fetchers.ticketmaster.venues.brixton-academy.id", "KovZ91777af")
                .withProperty("fetchers.ticketmaster.venues.eventim-apollo.id", "KovZpZAtadaA");

        registrar.setEnvironment(env);
        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry).registerBeanDefinition(eq("brixton-academyTicketmasterEventRouteBuilder"), any());
        verify(registry).registerBeanDefinition(eq("eventim-apolloTicketmasterEventRouteBuilder"), any());
        verifyNoMoreInteractions(registry);
    }

    @Test
    void doesNotRegisterBeansWhenApiKeyAbsent() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("fetchers.ticketmaster.venues.brixton-academy.id", "KovZ91777af");

        registrar.setEnvironment(env);
        registrar.postProcessBeanDefinitionRegistry(registry);

        verifyNoInteractions(registry);
    }

    @Test
    void doesNotRegisterBeansWhenNoVenuesConfigured() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("fetchers.ticketmaster.api-key", "test-key");

        registrar.setEnvironment(env);
        registrar.postProcessBeanDefinitionRegistry(registry);

        verifyNoInteractions(registry);
    }

    @Test
    void skipsVenueWithMissingId() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("fetchers.ticketmaster.api-key", "test-key")
                .withProperty("fetchers.ticketmaster.venues.brixton-academy.id", "KovZ91777af")
                .withProperty("fetchers.ticketmaster.venues.no-id-venue.other-prop", "ignored");

        registrar.setEnvironment(env);
        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry, times(1)).registerBeanDefinition(any(), any());
        verify(registry).registerBeanDefinition(eq("brixton-academyTicketmasterEventRouteBuilder"), any());
    }
}
