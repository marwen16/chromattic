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

package org.chromattic.test.onetomany.reference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import java.util.Collection;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class PathOneToManyTestCase extends OneToManyTestCase<TOTMP_A_3, TOTMP_B_3> {

  protected Class<TOTMP_A_3> getOneSideClass() {
    return TOTMP_A_3.class;
  }

  protected Class<TOTMP_B_3> getManySideClass() {
    return TOTMP_B_3.class;
  }

  protected void createLink(Node referent, String propertyName, Node referenced) throws RepositoryException {
    if (referenced != null) {
      String path = referenced.getPath();
      ValueFactory valueFactory = referent.getSession().getValueFactory();
      Value value = valueFactory.createValue(path, PropertyType.PATH);
      referent.setProperty(propertyName, value);
    } else {
      referent.setProperty(propertyName, (String)null);
    }
  }

  protected Collection<TOTMP_B_3> getMany(TOTMP_A_3 one) {
    return one.getBs();
  }

  protected TOTMP_A_3 getOne(TOTMP_B_3 many) {
    return many.getA();
  }

  protected void setOne(TOTMP_B_3 many, TOTMP_A_3 one) {
    many.setA(one);
  }

  protected String getOneNodeType() {
    return "totmp_a";
  }

  protected String getManyNodeType() {
    return "totmp_b";
  }
}