package wypozyczalnia.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import java.time.Duration;
import java.util.Set;

/**
 * Implementacja cache'a używająca Redis jako backend. Obsługuje
 * serializację/deserializację obiektów do JSON.
 */
public class RedisCacheManager implements CacheManager {

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private volatile boolean available = true;

    /**
     * Konstruktor ładujący konfigurację z pliku properties.
     */
    public RedisCacheManager() {
        Properties props = new Properties();
        // Domyślne wartości
        final String defaultHost = "localhost";
        final int defaultPort = 6379;
        final int maxTotal = 20;
        final int maxIdle = 10;
        final int minIdle = 1;
        final boolean testOnBorrow = true;
        final boolean testWhileIdle = true;
        final long maxWaitMillis = 3000;
        String uriString = null;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("redis.properties")) {
            if (input != null) {
                props.load(input);
                // Odczytujemy tylko URI
                uriString = props.getProperty("redis.uri");
            } else {
                System.err.println("Nie znaleziono pliku redis.properties");
            }
        } catch (IOException e) {
            System.err.println("Błąd ładowania pliku redis.properties: " + e.getMessage());
        }

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestWhileIdle(testWhileIdle);
        config.setMaxWaitMillis(maxWaitMillis);

        JedisPool pool;
        if (uriString != null && !uriString.isBlank()) {
            try {
                URI uri = URI.create(uriString);
                pool = new JedisPool(config, uri);
            } catch (Exception e) {
                System.err.println("Błąd parsowania redis.uri, fallback na domyślny host+port: " + e.getMessage());
                pool = new JedisPool(config, defaultHost, defaultPort, 3000);
            }
        } else {
            pool = new JedisPool(config, defaultHost, defaultPort, 3000);
        }
        this.jedisPool = pool;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        testConnection();
    }

    private void testConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            this.available = true;
        } catch (Exception e) {
            this.available = false;
            System.err.println("Redis niedostępny: " + e.getMessage());
        }
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (!isAvailable()) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(value);
            long ttlSeconds = ttl.getSeconds();

            if (ttlSeconds > 0) {
                jedis.setex(key, (int) ttlSeconds, json);
            } else {
                jedis.set(key, json);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Błąd serializacji do cache: " + e.getMessage());
        } catch (JedisConnectionException e) {
            this.available = false;
            System.err.println("Utrata połączenia z Redis: " + e.getMessage());
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        if (!isAvailable()) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            System.err.println("Błąd deserializacji z cache: " + e.getMessage());
            return null;
        } catch (JedisConnectionException e) {
            this.available = false;
            System.err.println("Utrata połączenia z Redis: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String key) {
        if (!isAvailable()) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (JedisConnectionException e) {
            this.available = false;
            System.err.println("Utrata połączenia z Redis: " + e.getMessage());
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        if (!isAvailable()) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(prefix + "*");
            if (!keys.isEmpty()) {
                String[] keysArray = keys.toArray(String[]::new);
                jedis.del(keysArray);
            }
        } catch (JedisConnectionException e) {
            this.available = false;
            System.err.println("Utrata połączenia z Redis: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        if (!isAvailable()) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (JedisConnectionException e) {
            this.available = false;
            System.err.println("Utrata połączenia z Redis: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        if (!available) {
            // Spróbuj odnowić połączenie co pewien czas
            testConnection();
        }
        return available;
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
