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

package org.chromattic.test.onetoone.embedded;

import org.chromattic.api.RelationshipType;
import org.chromattic.api.annotations.Owner;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.MappedBy;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@PrimaryType(name = "onetoone_e:b")
public abstract class B {

  @OneToOne
  @MappedBy("b")
  public abstract C getParent();

  public abstract void setParent(C b);

  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract C getMixin();

  public abstract void setMixin(C b);

  @OneToOne(type = RelationshipType.EMBEDDED)
  @Owner
  public abstract A getSuperType();

  public abstract void setSuperType(A a);

}
