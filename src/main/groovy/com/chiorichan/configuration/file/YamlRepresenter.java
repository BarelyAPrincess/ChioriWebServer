/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.configuration.file;

import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.serialization.ConfigurationSerializable;
import com.chiorichan.configuration.serialization.ConfigurationSerialization;

public class YamlRepresenter extends Representer
{
	public YamlRepresenter()
	{
		this.multiRepresenters.put( ConfigurationSection.class, new RepresentConfigurationSection() );
		this.multiRepresenters.put( ConfigurationSerializable.class, new RepresentConfigurationSerializable() );
	}
	
	private class RepresentConfigurationSection extends RepresentMap
	{
		@Override
		public Node representData( Object data )
		{
			return super.representData( ( ( ConfigurationSection ) data ).getValues( false ) );
		}
	}
	
	private class RepresentConfigurationSerializable extends RepresentMap
	{
		@Override
		public Node representData( Object data )
		{
			ConfigurationSerializable serializable = ( ConfigurationSerializable ) data;
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			values.put( ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias( serializable.getClass() ) );
			values.putAll( serializable.serialize() );
			
			return super.representData( values );
		}
	}
}
