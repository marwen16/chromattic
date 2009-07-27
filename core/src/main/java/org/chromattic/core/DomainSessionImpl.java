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
package org.chromattic.core;

import org.chromattic.common.logging.Logger;
import org.chromattic.common.JCR;
import org.chromattic.api.Status;
import org.chromattic.api.DuplicateNameException;
import org.chromattic.core.mapper.TypeMapper;
import org.chromattic.core.jcr.SessionWrapper;
import org.chromattic.core.jcr.NodeDef;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NodeType;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class DomainSessionImpl extends DomainSession {

  /** . */
  final Domain domain;

  /** . */
  private final SessionWrapper sessionWrapper;

  /** . */
  private final Map<String, ObjectContext> contexts;

  /** . */
  private final Logger log = Logger.getLogger(DomainSession.class);

  public DomainSessionImpl(Domain domain, SessionWrapper sessionWrapper) {
    super(domain);

    //
    this.domain = domain;
    this.sessionWrapper = sessionWrapper;
    this.contexts = new HashMap<String, ObjectContext>();
  }

  public Session getJCRSession() {
    return sessionWrapper.getSession();
  }

  protected <O> O _findByPath(Object o, Class<O> clazz, String relPath) throws RepositoryException {
    Node origin;
    if (o != null) {
      ObjectContext ctx = unwrap(o);
      origin = ctx.state.getNode();
    } else {
      origin = sessionWrapper.getSession().getRootNode();
      nodeRead(origin);
    }
    try {
      Node node = origin.getNode(relPath);
      nodeRead(node);
      return findByNode(clazz, node);
    }
    catch (PathNotFoundException e) {
      return null;
    }
  }

  protected String _persist(ObjectContext ctx, String relPath) throws RepositoryException {
    if (ctx == null) {
      throw new NullPointerException("No null object context accepted");
    }
    if (relPath == null) {
      throw new NullPointerException("No relative path specified");
    }

    //
    if (ctx.getStatus() != Status.TRANSIENT) {
      String msg = "Attempt to persist non transient object " + ctx;
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    //
    NodeDef nodeDef = ctx.mapper.getNodeDef();
    log.trace("Setting context {} for insertion", ctx);
    log.trace("Adding node for context {} and node type {}", ctx, nodeDef);
    Node node = sessionWrapper.addNode(relPath, nodeDef);
    nodeAdded(node, ctx);
    String id = node.getUUID();
    log.trace("Added context {} for id {}", ctx, id);
    return id;
  }

  /**
   * Insert a child context as a name of a parent context, the name must be
   *
   * @param parentCtx the parent context
   * @param relPath the child name
   * @param childCtx the child context
   * @return the id of the inserted context
   * @throws NullPointerException
   * @throws IllegalArgumentException
   * @throws IllegalStateException
   * @throws RepositoryException
   */
  protected String _persist(ObjectContext parentCtx, String relPath, ObjectContext childCtx) throws
    NullPointerException,
    IllegalArgumentException,
    IllegalStateException,
    RepositoryException {
    if (parentCtx == null) {
      String msg = "Cannot insert context " + childCtx + " as a child of a null context";
      log.error(msg);
      throw new NullPointerException(msg);
    }
    if (childCtx.getStatus() != Status.TRANSIENT) {
      String msg = "Attempt to insert transient context " + childCtx + " as child of " + parentCtx;
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    if (relPath == null) {
      String msg = "Attempt to insert context " + childCtx + " with no relative path to " + parentCtx;
      log.error(msg);
      throw new NullPointerException(msg);
    }
    if (parentCtx.getStatus() != Status.PERSISTENT) {
      String msg = "Attempt to insert context " + childCtx + " as child of non persistent context " + parentCtx;
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    //
    Node parentNode = parentCtx.state.getNode();

    //
    if (parentNode.hasNode(relPath)) {
      String msg = "Attempt to insert context " + childCtx + " as an existing child with name " + relPath + " child of context " + parentCtx;
      log.error(msg);
      throw new DuplicateNameException(msg);
    }

    //
    NodeDef nodeDef = childCtx.mapper.getNodeDef();
    log.trace("Setting context {} for insertion", childCtx);
    log.trace("Adding node for context {} and node type {} as child of context {}", childCtx, nodeDef, parentCtx);
    Node child = sessionWrapper.addNode(parentNode, relPath, nodeDef);
    nodeAdded(child, childCtx);
    String childId = child.getUUID();

    //
    log.trace("Added context {} for id {}", childCtx, childId);
    return childId;
  }

  protected <O> O _create(Class<O> clazz, String name) throws NullPointerException, IllegalArgumentException {
    if (clazz == null) {
      throw new NullPointerException();
    }

    // Validate the name
    if (name != null) {
      JCR.validateName(name);
    }

    //
    TypeMapper typeMapper = domain.getTypeMapper(clazz);
    ObjectContext ctx = new ObjectContext(typeMapper, name);
    fireEvent(LifeCycleType.CREATED, ctx);
    return (O)ctx.getObject();
  }

  protected <O> O _findById(Class<O> clazz, String id) throws RepositoryException {
    if (clazz == null) {
      throw new NullPointerException();
    }
    if (id == null) {
      throw new NullPointerException();
    }

    //
    ObjectContext ctx = contexts.get(id);

    // Attempt to load the object
    if (ctx == null) {
      try {
        log.trace("About to load node with id {} and class {}", id, clazz.getName());
        Node node = sessionWrapper.getNodeByUUID(id);
        nodeRead(node);
        log.trace("Loaded node with id {}", id, clazz.getName());
        ctx = contexts.get(id);
        log.trace("Obtained context {} node for id {} and class {}", ctx, id, clazz.getName());
      }
      catch (ItemNotFoundException e) {
        log.trace("Could not find node with id {}", id, clazz.getName());
        return null;
      }
      catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    }

    //
    if (ctx == null) {
      return null;
    } else {
      Object object = ctx.object;
      if (clazz.isInstance(object)) {
        return clazz.cast(object);
      } else {
        String msg = "Could not cast context " + ctx + " with class " + object.getClass().getName() + " to class " + clazz.getName();
        throw new ClassCastException(msg);
      }
    }
  }

  protected void _save() throws RepositoryException {
    sessionWrapper.save();
  }

  protected void _remove(Object o) throws RepositoryException {
    if (o == null) {
      throw new NullPointerException();
    }
    ObjectContext context = unwrap(o);
    _remove(context);
  }

  protected void _remove(ObjectContext context) throws RepositoryException {
    if (context == null) {
      throw new NullPointerException();
    }
    switch (context.state.getStatus()) {
      case TRANSIENT:
        throw new IllegalStateException("Cannot remove transient node");
      case PERSISTENT:
        Node node = context.state.getNode();
        remove(node);
        break;
      case REMOVED:
        throw new IllegalStateException("Cannot remove removed node");
      default:
        throw new AssertionError();
    }
  }

  private void remove(Node node) throws RepositoryException {
    Iterator<String> ids = sessionWrapper.remove(node);
    while (ids.hasNext()) {
      String id = ids.next();
      nodeRemoved(id);
    }
  }

  protected Object _getRelated(ObjectContext ctx, String name) throws RepositoryException {
    if (ctx.getStatus() != Status.PERSISTENT) {
      throw new IllegalStateException();
    }
    Node node = ctx.state.getNode();
    Node related = sessionWrapper.getRelated(node, name);
    if (related != null) {
      return findByNode(Object.class, related);
    } else {
      return null;
    }
  }

  protected boolean _setRelated(ObjectContext ctx, String name, ObjectContext relatedCtx) throws RepositoryException {
    if (ctx.getStatus() != Status.PERSISTENT) {
      throw new IllegalStateException("Cannot create a relationship with a non persisted context " + this);
    }

    //
    Node node = ctx.state.getNode();

    // Then create
    if (relatedCtx != null) {
      if (relatedCtx.getStatus() != Status.PERSISTENT) {
        throw new IllegalStateException();
      }

      // Should do some type checking probably!!!!

      //
      Node relatedNode = relatedCtx.state.getNode();

      //
      return relatedNode != sessionWrapper.setRelated(node, name, relatedNode);
    } else {
      return null != sessionWrapper.setRelated(node, name, null);
    }
  }

  protected <T> Iterator<T> _getRelateds(ObjectContext ctx, String name, Class<T> filterClass) throws RepositoryException {
    Node node = ctx.state.getNode();
    Iterator<Node> nodes = sessionWrapper.getRelateds(node, name);
    return new ReferentCollectionIterator<T>(this, nodes, filterClass, name);
  }

  protected void _removeChild(ObjectContext ctx, String name) throws RepositoryException {
    Node node = ctx.state.getNode();
    if (node.hasNode(name)) {
      Node childNode = node.getNode(name);
      remove(childNode);
    }
  }

  protected Object _getChild(ObjectContext ctx, String name) throws RepositoryException {
    Node node = ctx.state.getNode();
    log.trace("About to load the name child {} of context {}", name, this);
    Node child = sessionWrapper.getChild(node, name);
    if (child != null) {
      log.trace("Loaded named child {} of context {} with id {}", name, this, child.getUUID());
      return findByNode(Object.class, child);
    } else {
      log.trace("No child named {} to load for context {}", name, this);
      return null;
    }
  }

  protected <T> Iterator<T> _getChildren(ObjectContext ctx, Class<T> filterClass) throws RepositoryException {
    Node node = ctx.state.getNode();
    Iterator<Node> iterator = sessionWrapper.getChildren(node);
    return new ChildCollectionIterator<T>(this, iterator, filterClass);
  }

  protected Object _getParent(ObjectContext ctx) throws RepositoryException {
    if (ctx.getStatus() != Status.PERSISTENT) {
      throw new IllegalStateException();
    }
    Node node = ctx.state.getNode();
    Node parent = sessionWrapper.getParent(node);
    return findByNode(Object.class, parent);
  }

  public void nodeRead(Node node) throws RepositoryException {
    NodeType nodeType = node.getPrimaryNodeType();
    String nodeTypeName = nodeType.getName();
    TypeMapper mapper = domain.getTypeMapper(nodeTypeName);
    if (mapper != null) {
      String id = node.getUUID();
      ObjectContext ctx = contexts.get(id);
      if (ctx == null) {
        ctx = new ObjectContext(mapper);
        log.trace("Inserted context {} loaded from node id {}", ctx, id);
        contexts.put(id, ctx);
        ctx.state = new ObjectState.Persistent(node, this);
        fireEvent(LifeCycleType.LOADED, ctx);
      }
      else {
        log.trace("Context {} is already present for id ", ctx, id);
      }
    }
    else {
      log.trace("Could not find mapper for node type {}", nodeTypeName);
    }
  }

  public void nodeAdded(Node node, ObjectContext ctx) throws RepositoryException {
    NodeType nodeType = node.getPrimaryNodeType();
    String nodeTypeName = nodeType.getName();
    TypeMapper mapper = domain.getTypeMapper(nodeTypeName);
    if (mapper != null) {
      String id = node.getUUID();
      if (contexts.containsKey(id)) {
        String msg = "Attempt to replace an existing context " + ctx + " with id " + id;
        log.error(msg);
        throw new AssertionError(msg);
      }
      log.trace("Inserted context {} for id {}", ctx, id);
      contexts.put(id, ctx);
      ctx.state = new ObjectState.Persistent(node, this);
      fireEvent(LifeCycleType.PERSISTED, ctx);
    }
    else {
      log.trace("Could not find mapper for node type {}", nodeTypeName);
    }
  }

  public void nodeRemoved(String nodeId) throws RepositoryException {
    log.trace("Removing context for id {}", nodeId);
    ObjectContext ctx = contexts.remove(nodeId);
    ctx.state = new ObjectState.Removed(nodeId);
    fireEvent(LifeCycleType.REMOVED, ctx);
    log.trace("Removed context {} for id {}", ctx, nodeId);
  }

  public void close() {
    sessionWrapper.close();
  }
}
