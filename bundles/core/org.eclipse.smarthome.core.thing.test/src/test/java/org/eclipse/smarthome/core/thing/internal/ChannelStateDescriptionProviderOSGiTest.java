/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.thing.internal;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ChannelStateDescriptionProvider}.
 *
 * @author Alex Tugarev - Initial contribution
 * @author Thomas Höfer - Thing type constructor modified because of thing properties introduction
 * @author Markus Rathgeb - Migrated from Groovy to plain Java
 */
public class ChannelStateDescriptionProviderOSGiTest extends JavaOSGiTest {

    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry linkRegistry;
    private StateDescriptionProvider stateDescriptionProvider;

    @Mock
    private ComponentContext componentContext;

    @Before
    public void setup() {
        initMocks(this);

        Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);

        registerVolatileStorageService();

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        final TestThingHandlerFactory thingHandlerFactory = new TestThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        final StateDescription state = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.TEN,
                "%d Peek", true, Collections.singletonList(new StateOption("SOUND", "My great sound.")));

        final StateDescription state2 = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(256),
                BigDecimal.valueOf(8), null, false, null);

        final ChannelType channelType = new ChannelType(new ChannelTypeUID("hue:alarm"), false, "Number", " ", "", null,
                null, state, null);
        final ChannelType channelType2 = new ChannelType(new ChannelTypeUID("hue:num"), false, "Number", " ", "", null,
                null, state2, null);
        final ChannelType channelType3 = new ChannelType(new ChannelTypeUID("hue:info"), true, "String", " ", "", null,
                null, null, null);
        final ChannelType channelType4 = new ChannelType(new ChannelTypeUID("hue:color"), false, "Color", "Color", "",
                "ColorLight", null, null, null);
        final ChannelType channelType5 = new ChannelType(new ChannelTypeUID("hue:brightness"), false, "Dimmer",
                "Brightness", "", "DimmableLight", null, null, null);
        final ChannelType channelType6 = new ChannelType(new ChannelTypeUID("hue:switch"), false, "Switch", "Switch",
                "", "Light", null, null, null);

        List<ChannelType> channelTypes = new ArrayList<>();
        channelTypes.add(channelType);
        channelTypes.add(channelType2);
        channelTypes.add(channelType3);
        channelTypes.add(channelType4);
        channelTypes.add(channelType5);
        channelTypes.add(channelType6);

        registerService(new ChannelTypeProvider() {
            @Override
            public Collection<ChannelType> getChannelTypes(Locale locale) {
                return channelTypes;
            }

            @Override
            public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
                for (final ChannelType channelType : channelTypes) {
                    if (channelType.getUID().equals(channelTypeUID)) {
                        return channelType;
                    }
                }
                return null;
            }

            @Override
            public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
                return null;
            }

            @Override
            public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
                return Collections.emptySet();
            }
        });

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        channelDefinitions.add(new ChannelDefinition("1", channelType.getUID()));
        channelDefinitions.add(new ChannelDefinition("2", channelType2.getUID()));
        channelDefinitions.add(new ChannelDefinition("3", channelType3.getUID()));
        channelDefinitions.add(new ChannelDefinition("4", channelType4.getUID()));
        channelDefinitions.add(new ChannelDefinition("5", channelType5.getUID()));
        channelDefinitions.add(new ChannelDefinition("6", channelType6.getUID()));

        registerService(new SimpleThingTypeProvider(Collections.singleton(
                new ThingType(new ThingTypeUID("hue:lamp"), null, " ", null, channelDefinitions, null, null, null))));

        List<Item> items = new ArrayList<>();
        items.add(new NumberItem("TestItem"));
        items.add(new NumberItem("TestItem2"));
        items.add(new StringItem("TestItem3"));
        items.add(new ColorItem("TestItem4"));
        items.add(new DimmerItem("TestItem5"));
        items.add(new SwitchItem("TestItem6"));
        registerService(new TestItemProvider(items));

        linkRegistry = getService(ItemChannelLinkRegistry.class);

        stateDescriptionProvider = getService(StateDescriptionProvider.class);
        assertNotNull(stateDescriptionProvider);
    }

    @After
    public void teardown() {
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        managedThingProvider.getAll().forEach(thing -> {
            managedThingProvider.remove(thing.getUID());
        });
        linkRegistry.getAll().forEach(link -> {
            linkRegistry.remove(link.getUID());
        });
    }

    /**
     * Assert that item's state description is present.
     */
    @Test
    public void presentItemStateDescription() throws ItemNotFoundException {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);

        Thing thing = thingRegistry.createThingOfType(new ThingTypeUID("hue:lamp"), new ThingUID("hue:lamp:lamp1"),
                null, "test thing", new Configuration());
        assertNotNull(thing);
        managedThingProvider.add(thing);
        ItemChannelLink link = new ItemChannelLink("TestItem", thing.getChannel("1").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem2", thing.getChannel("2").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem3", thing.getChannel("3").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem4", thing.getChannel("4").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem5", thing.getChannel("5").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem6", thing.getChannel("6").getUID());
        linkRegistry.add(link);
        //
        final Collection<Item> items = itemRegistry.getItems();
        assertEquals(false, items.isEmpty());

        Item item = itemRegistry.getItem("TestItem");
        assertEquals("Number", item.getType());

        StateDescription state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.ZERO, state.getMinimum());
        assertEquals(BigDecimal.valueOf(100), state.getMaximum());
        assertEquals(BigDecimal.TEN, state.getStep());
        assertEquals("%d Peek", state.getPattern());
        assertEquals(true, state.isReadOnly());
        List<StateOption> opts = state.getOptions();
        assertEquals(1, opts.size());
        final StateOption opt = opts.get(0);
        assertEquals("SOUND", opt.getValue());
        assertEquals("My great sound.", opt.getLabel());

        item = itemRegistry.getItem("TestItem2");
        assertEquals("Number", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.ZERO, state.getMinimum());
        assertEquals(BigDecimal.valueOf(256), state.getMaximum());
        assertEquals(BigDecimal.valueOf(8), state.getStep());
        assertEquals("%.0f", state.getPattern());
        assertEquals(false, state.isReadOnly());
        opts = state.getOptions();
        assertEquals(0, opts.size());

        item = itemRegistry.getItem("TestItem3");
        assertEquals("String", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertNull(state.getMinimum());
        assertNull(state.getMaximum());
        assertNull(state.getStep());
        assertEquals("%s", state.getPattern());
        assertEquals(false, state.isReadOnly());
        opts = state.getOptions();
        assertEquals(0, opts.size());

        item = itemRegistry.getItem("TestItem4");
        assertEquals("Color", item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem5");
        assertEquals("Dimmer", item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem6");
        assertEquals("Switch", item.getType());

        state = item.getStateDescription();
        assertNull(state);

    }

    /*
     * Helper
     */

    class TestThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public void activate(final ComponentContext ctx) {
            super.activate(ctx);
        }

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected ThingHandler createHandler(Thing thing) {
            return new BaseThingHandler(thing) {
                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                }
            };
        }
    }

    class TestItemProvider implements ItemProvider {
        private Collection<Item> items;

        TestItemProvider(Collection<Item> items) {
            this.items = items;
        }

        @Override
        public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
        }

        @Override
        public Collection<Item> getAll() {
            return items;
        }

        @Override
        public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
        }
    }
}