package org.janus.sidecar.persistence;

import org.janus.sidecar.configuration.properties.SidecarProperties;
import org.janus.sidecar.registry.ActualDegradationRegistry;
import org.janus.sidecar.registry.InMemoryActualDegradationRegistry;
import org.janus.sidecar.registry.PersistentActualDegradationRegistry;
import org.jspecify.annotations.NullMarked;
import org.sqlite.SQLiteDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NullMarked
public class PersistenceConfiguration {

  @Bean
  public SQLiteDataSource sqLiteDataSource(SidecarProperties properties) {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + properties.sqlitePath());
    dataSource.setJournalMode("WAL");
    return dataSource;
  }

  @Bean
  public DegradationIdStore degradationIdStore(SQLiteDataSource dataSource) {
    var store = new DegradationIdStore(dataSource);
    store.init();
    return store;
  }

  @Bean
  public InMemoryActualDegradationRegistry inMemoryActualDegradationRegistry() {
    return new InMemoryActualDegradationRegistry();
  }

  @Bean
  public ActualDegradationRegistry actualDegradationRegistry(
      InMemoryActualDegradationRegistry inMemory, DegradationIdStore store) {
    return new PersistentActualDegradationRegistry(inMemory, store);
  }
}
