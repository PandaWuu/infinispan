package org.infinispan.query.impl.massindex;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.infinispan.AdvancedCache;
import org.infinispan.query.backend.KeyTransformationHandler;
import org.infinispan.query.impl.ComponentRegistryUtils;
import org.infinispan.query.logging.Log;
import org.infinispan.search.mapper.mapping.SearchMapping;
import org.infinispan.util.logging.LogFactory;

import net.jcip.annotations.NotThreadSafe;

/**
 * Handle batch updates to an index.
 *
 * @author gustavonalle
 * @since 7.1
 */
@NotThreadSafe
public class IndexUpdater {

   private static final Log LOG = LogFactory.getLog(IndexUpdater.class, Log.class);

   private final AdvancedCache<?, ?> cache;
   private final KeyTransformationHandler keyTransformationHandler;

   private SearchMapping searchMapping;

   public IndexUpdater(AdvancedCache<?, ?> cache, KeyTransformationHandler keyTransformationHandler) {
      this.cache = cache;
      this.keyTransformationHandler = keyTransformationHandler;
   }

   public IndexUpdater(SearchMapping searchMapping, KeyTransformationHandler keyTransformationHandler) {
      this.cache = null;
      this.keyTransformationHandler = keyTransformationHandler;
      this.searchMapping = searchMapping;
   }

   public void flush(Collection<Class<?>> javaClasses) {
      if (javaClasses.isEmpty()) {
         return;
      }

      LOG.flushingIndex(javaClasses.toString());
      getSearchMapping().scopeFromJavaClasses(javaClasses).workspace().flush();
   }

   public void refresh(Collection<Class<?>> javaClasses) {
      if (javaClasses.isEmpty()) {
         return;
      }

      LOG.flushingIndex(javaClasses.toString());
      getSearchMapping().scopeFromJavaClasses(javaClasses).workspace().refresh();
   }

   public void purge(Collection<Class<?>> javaClasses) {
      if (javaClasses.isEmpty()) {
         return;
      }

      LOG.purgingIndex(javaClasses.toString());
      getSearchMapping().scopeFromJavaClasses(javaClasses).workspace().purge();
   }

   public Collection<Class<?>> allJavaClasses() {
      return getSearchMapping().allIndexedTypes().values();
   }

   public CompletableFuture<?> updateIndex(Object key, Object value, int segment) {
      if (value == null || Thread.currentThread().isInterrupted()) {
         return CompletableFuture.completedFuture(null);
      }

      final String idInString = keyTransformationHandler.keyToString(key);
      return getSearchMapping().getSearchIndexer().addOrUpdate(idInString, String.valueOf(segment), value);
   }

   private SearchMapping getSearchMapping() {
      if (searchMapping == null) {
         searchMapping = ComponentRegistryUtils.getSearchMapping(cache);
      }
      return searchMapping;
   }
}
