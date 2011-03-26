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

package org.chromattic.core.mapper.nodeattribute;

import org.chromattic.api.Status;
import org.chromattic.core.NodeAttributeType;
import org.chromattic.core.mapper.PropertyMapper;
import org.chromattic.core.ObjectContext;
import org.chromattic.core.bean.SingleValuedPropertyInfo;
import org.chromattic.core.bean.SimpleValueInfo;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class JCRNodeAttributePropertyMapper extends PropertyMapper<SingleValuedPropertyInfo<SimpleValueInfo>> {

  /** . */
  private final NodeAttributeType type;

  public JCRNodeAttributePropertyMapper(SingleValuedPropertyInfo<SimpleValueInfo> info, NodeAttributeType type) {
    super(info);

    //
    this.type = type;
  }

  @Override
  public Object get(ObjectContext context) throws Throwable {
    return context.getAttribute(type);
  }

  @Override
  public void set(ObjectContext context, Object value) {
    if (context.getStatus() == Status.REMOVED) {
      throw new IllegalStateException("Node is removed");
    }
    if (type == NodeAttributeType.NAME) {
      context.setName((String)value);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}