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

package org.chromattic.metamodel.mapping2;

import org.chromattic.api.annotations.*;
import org.chromattic.metamodel.bean2.*;
import org.chromattic.metamodel.mapping.jcr.PropertyDefinitionMapping;
import org.chromattic.metamodel.mapping.jcr.PropertyMetaType;
import org.chromattic.metamodel.type.SimpleTypeMapping;
import org.chromattic.metamodel.type.SimpleTypeResolver;
import org.reflext.api.ClassTypeInfo;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ApplicationMappingBuilder {



  public Map<ClassTypeInfo, BeanMapping> build(Set<ClassTypeInfo> classTypes) {

    Collection<BeanInfo> beans = new BeanInfoBuilder().build(classTypes).values();

    //
    Context ctx = new Context(new SimpleTypeResolver(), new HashSet<BeanInfo>(beans));

    //
    Map<BeanInfo, BeanMapping> beanMappings = ctx.build();

    //
    RelationshipResolver resolver = new RelationshipResolver(beanMappings);

    //
    resolver.resolve();

    //
    Map<ClassTypeInfo, BeanMapping> classTypeMappings = new HashMap<ClassTypeInfo, BeanMapping>();
    for (Map.Entry<BeanInfo, BeanMapping> beanMapping : beanMappings.entrySet()) {
      classTypeMappings.put(beanMapping.getKey().getClassType(), beanMapping.getValue());
    }

    //
    return classTypeMappings;
  }

  private class RelationshipResolver {

    /** . */
    Map<BeanInfo, BeanMapping> beanMappings;

    private RelationshipResolver(Map<BeanInfo, BeanMapping> beanMappings) {
      this.beanMappings = beanMappings;
    }

    private void resolve() {
      for (BeanMapping beanMapping : beanMappings.values()) {
        for (PropertyMapping propertyMapping : beanMapping.getProperties().values()) {
          if (propertyMapping instanceof RelationshipPropertyMapping<?>) {
            RelationshipPropertyMapping<?> relationshipMapping = (RelationshipPropertyMapping<?>)propertyMapping;
            BeanInfo relatedBean = relationshipMapping.getRelatedBean();
            BeanMapping relatedBeanMapping = beanMappings.get(relatedBean);
            Relationship relationship = relationshipMapping.getRelationship();
            if (relationship instanceof Relationship.OneToOne.Hierarchic) {
              Relationship.OneToOne.Hierarchic oneToOneHierarchicRelationship = (Relationship.OneToOne.Hierarchic)relationship;
              for (PropertyMapping relatedBeanPropertyMapping : relatedBeanMapping.getProperties().values()) {
                if (relatedBeanPropertyMapping instanceof RelationshipPropertyMapping) {
                  RelationshipPropertyMapping<?> relatedBeanRelationshipMapping = (RelationshipPropertyMapping<?>)relatedBeanPropertyMapping;
                  Relationship relatedBeanRelationship = ((RelationshipPropertyMapping) relatedBeanPropertyMapping).getRelationship();
                  if (relatedBeanRelationship instanceof Relationship.OneToOne.Hierarchic) {
                    Relationship.OneToOne.Hierarchic relatedBeanOneToOneHierarchicRelationship = (Relationship.OneToOne.Hierarchic)relatedBeanRelationship;
                    if (relatedBeanOneToOneHierarchicRelationship.getMappedBy().equals(oneToOneHierarchicRelationship.getMappedBy())) {
                      if (relationshipMapping != relatedBeanRelationshipMapping) {
                        if (relationshipMapping.related != null) {
                          throw new UnsupportedOperationException();
                        }
                        relationshipMapping.related = relatedBeanRelationshipMapping;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private class Context {

    /** . */
    final SimpleTypeResolver typeResolver;

    /** . */
    final Set<BeanInfo> beans;

    /** . */
    final Map<BeanInfo, BeanMapping> beanMappings;

    private Context(SimpleTypeResolver typeResolver, Set<BeanInfo> beans) {
      this.typeResolver = typeResolver;
      this.beans = beans;
      this.beanMappings = new HashMap<BeanInfo, BeanMapping>();
    }

    public Map<BeanInfo, BeanMapping> build() {
      while (true) {
        Iterator<BeanInfo> iterator = beans.iterator();
        if (iterator.hasNext()) {
          BeanInfo bean = iterator.next();
          resolve(bean);
        } else {
          return beanMappings;
        }
      }
    }

    private BeanMapping resolve(BeanInfo bean) {
      BeanMapping mapping = beanMappings.get(bean);
      if (mapping == null) {
        if (beans.remove(bean)) {
          mapping = new BeanMapping(bean);
          beanMappings.put(bean, mapping);
          build(mapping);
        } else {
          // It does not resolve
        }
      }
      return mapping;
    }

    private void build(BeanMapping beanMapping) {

      BeanInfo bean = beanMapping.bean;

      // First build the parent mapping if any
      if (bean.getParent() != null) {
        beanMapping.parent = resolve(bean.getParent());
      }

      //
      Map<String, PropertyMapping<?, ?>> properties = new HashMap<String, PropertyMapping<?, ?>>();
      for (PropertyInfo<?> property : bean.getProperties().values()) {

        // Determine kind
        Collection<? extends Annotation> annotations = property.getAnnotateds(
            Property.class,
            OneToOne.class,
            OneToMany.class,
            ManyToOne.class
        );

        //
        if (annotations.size() > 1) {
          throw new UnsupportedOperationException();
        }

        // Build the correct mapping or fail
        PropertyMapping<?, ?> mapping = null;
        if (annotations.size() == 1) {
          Annotation annotation = annotations.iterator().next();
          ValueInfo value = property.getValue();
          if (property instanceof SingleValuedPropertyInfo<?>) {
            if (value instanceof SimpleValueInfo) {
              if (annotation instanceof Property) {
                Property propertyAnnotation = (Property)annotation;
                mapping = createProperty(propertyAnnotation, (SingleValuedPropertyInfo<SimpleValueInfo>)property);
              } else {
                throw new UnsupportedOperationException();
              }
            } else if (value instanceof BeanValueInfo) {
              if (annotation instanceof OneToOne) {
                OneToOne oneToOne =  (OneToOne)annotation;
                switch (oneToOne.type()) {
                  case HIERARCHIC:
                    mapping = createHierarchicOneToOne((SingleValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  case EMBEDDED:
                    mapping = createEmbeddedOneToOne((SingleValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  default:
                    throw new UnsupportedOperationException();
                }
              } else if (annotation instanceof ManyToOne) {
                ManyToOne manyToOne = (ManyToOne)annotation;
                switch (manyToOne.type()) {
                  case HIERARCHIC:
                    mapping = createHierarchicManyToOne((SingleValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  case PATH:
                  case REFERENCE:
                    mapping = createReferenceManyToOne((SingleValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  default:
                    throw new UnsupportedOperationException();
                }
              } else {
                throw new UnsupportedOperationException();
              }
            } else {
              throw new AssertionError();
            }
          } else if (property instanceof MultiValuedPropertyInfo<?>) {
            if (value instanceof SimpleValueInfo) {
              if (annotation instanceof Property) {
                Property propertyAnnotation = (Property)annotation;
                mapping = createProperty(propertyAnnotation, (MultiValuedPropertyInfo<SimpleValueInfo>)property);
              } else {
                throw new UnsupportedOperationException();
              }
            } else if (value instanceof BeanValueInfo) {
              if (annotation instanceof OneToMany) {
                OneToMany oneToMany = (OneToMany)annotation;
                switch (oneToMany.type()) {
                  case HIERARCHIC:
                    mapping = createHierarchicOneToMany((MultiValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  case PATH:
                  case REFERENCE:
                    mapping = createReferenceOneToMany((MultiValuedPropertyInfo<BeanValueInfo>)property);
                    break;
                  default:
                    throw new UnsupportedOperationException();
                }
              } else {
                throw new UnsupportedOperationException();
              }
            } else {
              throw new AssertionError();
            }
          } else {
            throw new AssertionError();
          }
        }

        //
        properties.put(mapping.property.getName(), mapping);
      }

      //
      beanMapping.properties = Collections.unmodifiableMap(properties);
    }

    private <P extends PropertyInfo<SimpleValueInfo>> PropertyMapping<P, SimpleValueInfo> createProperty(
        Property propertyAnnotation,
        P property) {

      //
      PropertyMetaType<?> propertyMetaType = PropertyMetaType.get(propertyAnnotation.type());

      //
      SimpleTypeMapping abc = typeResolver.resolveType(property.getValue().getType(), propertyMetaType);
      if (abc == null) {
        throw new UnsupportedOperationException("No simple type mapping for " + property.getValue().getType());
      }

      //
      List<String> defaultValueList = null;
      DefaultValue defaultValueAnnotation = property.getAnnotation(DefaultValue.class);
      if (defaultValueAnnotation != null) {
        String[] defaultValues = defaultValueAnnotation.value();
        defaultValueList = new ArrayList<String>(defaultValues.length);
        defaultValueList.addAll(Arrays.asList(defaultValues));
        defaultValueList = Collections.unmodifiableList(defaultValueList);
      }

      //
      PropertyDefinitionMapping propertyDefinition = new PropertyDefinitionMapping(
          propertyAnnotation.name(),
          abc.getPropertyMetaType(),
          defaultValueList);

      //
      PropertyMapping<P, SimpleValueInfo> mapping;
      mapping = new SimplePropertyMapping<P>(property, propertyDefinition);
      return mapping;
    }

    private RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>> createReferenceOneToMany(MultiValuedPropertyInfo<BeanValueInfo> property) {
      RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>> mapping;
      mapping = new RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>>(property, new Relationship.OneToMany.Reference());
      return mapping;
    }

    private RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>> createHierarchicOneToMany(MultiValuedPropertyInfo<BeanValueInfo> property) {
      RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>> mapping;
      mapping = new RelationshipPropertyMapping<MultiValuedPropertyInfo<BeanValueInfo>>(property, new Relationship.OneToMany.Hierarchic());
      return mapping;
    }

    private RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> createReferenceManyToOne(SingleValuedPropertyInfo<BeanValueInfo> property) {
      RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> mapping;
      mapping = new RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>>(property, new Relationship.ManyToOne.Reference());
      return mapping;
    }

    private RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> createHierarchicManyToOne(SingleValuedPropertyInfo<BeanValueInfo> property) {
      RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> mapping;
      mapping = new RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>>(property, new Relationship.ManyToOne.Hierarchic());
      return mapping;
    }

    private RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> createEmbeddedOneToOne(SingleValuedPropertyInfo<BeanValueInfo> property) {
      RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> mapping;
      mapping = new RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>>(property, new Relationship.OneToOne.Embedded());
      return mapping;
    }

    private RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> createHierarchicOneToOne(
        SingleValuedPropertyInfo<BeanValueInfo> property) {
      MappedBy mappedBy = property.getAnnotation(MappedBy.class);
      if (mappedBy == null) {
        throw new UnsupportedOperationException();
      }
      boolean owner = property.getAnnotation(Owner.class) != null;
      RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>> mapping;
      Relationship.OneToOne.Hierarchic relationship = new Relationship.OneToOne.Hierarchic(owner, mappedBy.value());
      mapping = new RelationshipPropertyMapping<SingleValuedPropertyInfo<BeanValueInfo>>(property, relationship);
      return mapping;
    }
  }
}