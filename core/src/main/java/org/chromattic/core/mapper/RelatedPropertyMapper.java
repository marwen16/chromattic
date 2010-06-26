/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.chromattic.core.mapper;


import org.chromattic.core.ObjectContext;
import org.chromattic.metamodel.bean.PropertyInfo;
import org.chromattic.metamodel.bean.ValueInfo;
import org.chromattic.metamodel.mapping.PropertyMapping;

import java.util.Set;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class RelatedPropertyMapper<P extends PropertyInfo<V>, V extends ValueInfo, C extends ObjectContext> extends PropertyMapper<P, V, C> {

  /** . */
  protected Set<ObjectMapper> relatedTypes;

  protected RelatedPropertyMapper(Class<C> contextType, PropertyMapping<P, V> info) {
    super(contextType, info);
  }

  public abstract Class<?> getRelatedClass();

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[name=" + info.getProperty().getName() + "]";
  }
}