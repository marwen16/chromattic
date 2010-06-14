/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.chromattic.metamodel.bean2;

import org.chromattic.metamodel.bean.AnnotatedProperty;
import org.reflext.api.ClassTypeInfo;
import org.reflext.api.MethodInfo;
import org.reflext.api.TypeInfo;
import org.reflext.api.annotation.AnnotationType;
import org.reflext.api.introspection.AnnotationIntrospector;
import org.reflext.api.introspection.AnnotationTarget;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class PropertyInfo<V extends ValueInfo> {

  /** The bean this property is declared on. */
  private final BeanInfo bean;

  /** The parent property. */
  private PropertyInfo parent;

  /** The property name. */
  private final String name;

  /** The the most adapter getter. */
  private final MethodInfo getter;

  /** The the most adapted setter. */
  private final MethodInfo setter;

  /** . */
  private final V value;

  PropertyInfo(
      BeanInfo bean,
      PropertyInfo parent,
      String name,
      MethodInfo getter,
      MethodInfo setter,
      V value) {
    this.bean = bean;
    this.parent = parent;
    this.name = name;
    this.getter = getter;
    this.setter = setter;
    this.value = value;
  }

  public V getValue() {
    return value;
  }

  public BeanInfo getBean() {
    return bean;
  }

  public PropertyInfo getParent() {
    return parent;
  }

  public String getName() {
    return name;
  }

  public MethodInfo getGetter() {
    return getter;
  }

  public MethodInfo getSetter() {
    return setter;
  }

  public Collection<? extends Annotation> getAnnotateds(Class<? extends Annotation>... annotationClassTypes) {
    List<Annotation> props = new ArrayList<Annotation>();
    for (Class<? extends Annotation> annotationClassType : annotationClassTypes) {
      Annotation annotation = getAnnotation(annotationClassType);
      if (annotation != null) {
        props.add(annotation);
      }
    }
    return props;
  }

  public <A extends Annotation> A getAnnotation(Class<A> annotationClassType) {
    if (annotationClassType == null) {
      throw new NullPointerException();
    }

    //
    AnnotationTarget<MethodInfo, A> annotation = null;

    //
    AnnotationType<A, ?> annotationType = AnnotationType.get(annotationClassType);

    //
    if (getter != null) {
      annotation = new AnnotationIntrospector<A>(annotationType).resolve(getter);
    }

    //
    if (setter != null) {
      AnnotationTarget<MethodInfo, A> setterAnnotation = new AnnotationIntrospector<A>(annotationType).resolve(setter);
      if (setterAnnotation != null) {
        if (annotation != null) {
          throw new IllegalStateException("The same annotation " + annotation + " is present on a getter " +
            getter + " and setter" + setter);
        }
        annotation = setterAnnotation;
      }
    }

    //
    if (annotation != null) {
      return annotation.getAnnotation();
    } else {
      return null;
    }
  }
}