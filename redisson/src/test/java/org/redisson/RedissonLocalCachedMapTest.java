package org.redisson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.redisson.RedissonLocalCachedMap.CacheKey;
import org.redisson.RedissonLocalCachedMap.CacheValue;
import org.redisson.RedissonMapTest.SimpleKey;
import org.redisson.RedissonMapTest.SimpleValue;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.LocalCachedMapOptions.EvictionPolicy;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.misc.Cache;

import mockit.Deencapsulation;

public class RedissonLocalCachedMapTest extends BaseTest {

//    @Test
    public void testPerf() {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(100000).invalidateEntryOnChange(true);
        Map<String, Integer> map = redisson.getLocalCachedMap("test", options);
        
//        Map<String, Integer> map = redisson.getMap("test");

        
        for (int i = 0; i < 100000; i++) {
            map.put("" + i, i);
        }
        
        long s = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100000; j++) {
                map.get("" + j);
            }
        }
        System.out.println(System.currentTimeMillis() - s);

    }
    
    @Test
    public void testClearEmpty() {
        RLocalCachedMap<Object, Object> localCachedMap = redisson.getLocalCachedMap("udi-test",
                        LocalCachedMapOptions.defaults());

        localCachedMap.clear();
    }
    
    @Test
    public void testDelete() {
        RLocalCachedMap<String, String> localCachedMap = redisson.getLocalCachedMap("udi-test",
                        LocalCachedMapOptions.defaults());

        assertThat(localCachedMap.delete()).isFalse();
        localCachedMap.put("1", "2");
        assertThat(localCachedMap.delete()).isTrue();
    }

    @Test
    public void testInvalidationOnClear() throws InterruptedException {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5).invalidateEntryOnChange(true);
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        
        RLocalCachedMap<String, Integer> map2 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache2 = Deencapsulation.getField(map2, "cache");
        
        map1.put("1", 1);
        map1.put("2", 2);
        map2.put("3", 2);
        map2.put("4", 2);
        
        assertThat(map1.size()).isEqualTo(4);
        assertThat(map2.size()).isEqualTo(4);
        
        assertThat(map1.readAllEntrySet()).hasSize(4);
        assertThat(map2.readAllEntrySet()).hasSize(4);
        
        assertThat(cache1.size()).isEqualTo(4);
        assertThat(cache2.size()).isEqualTo(4);
        
        map1.clear();
        
        Thread.sleep(50);

        assertThat(cache1.size()).isZero();
        assertThat(cache2.size()).isZero();
        
        assertThat(map1.size()).isZero();
        assertThat(map2.size()).isZero();
    }
    
    @Test
    public void testInvalidationOnUpdate() throws InterruptedException {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5).invalidateEntryOnChange(true);
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        
        RLocalCachedMap<String, Integer> map2 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache2 = Deencapsulation.getField(map2, "cache");
        
        map1.put("1", 1);
        map1.put("2", 2);
        
        assertThat(map2.get("1")).isEqualTo(1);
        assertThat(map2.get("2")).isEqualTo(2);
        
        assertThat(cache1.size()).isEqualTo(2);
        assertThat(cache2.size()).isEqualTo(2);

        map1.put("1", 3);
        map2.put("2", 4);
        Thread.sleep(50);
        
        assertThat(cache1.size()).isEqualTo(1);
        assertThat(cache2.size()).isEqualTo(1);
    }
    
    @Test
    public void testNoInvalidationOnUpdate() throws InterruptedException {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5).invalidateEntryOnChange(false);
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        
        RLocalCachedMap<String, Integer> map2 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache2 = Deencapsulation.getField(map2, "cache");
        
        map1.put("1", 1);
        map1.put("2", 2);
        
        assertThat(map2.get("1")).isEqualTo(1);
        assertThat(map2.get("2")).isEqualTo(2);
        
        assertThat(cache1.size()).isEqualTo(2);
        assertThat(cache2.size()).isEqualTo(2);

        map1.put("1", 3);
        map2.put("2", 4);
        Thread.sleep(50);
        
        assertThat(cache1.size()).isEqualTo(2);
        assertThat(cache2.size()).isEqualTo(2);
    }
    
    @Test
    public void testNoInvalidationOnRemove() throws InterruptedException {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5).invalidateEntryOnChange(false);
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        
        RLocalCachedMap<String, Integer> map2 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache2 = Deencapsulation.getField(map2, "cache");
        
        map1.put("1", 1);
        map1.put("2", 2);
        
        assertThat(map2.get("1")).isEqualTo(1);
        assertThat(map2.get("2")).isEqualTo(2);
        
        assertThat(cache1.size()).isEqualTo(2);
        assertThat(cache2.size()).isEqualTo(2);

        map1.remove("1");
        map2.remove("2");
        Thread.sleep(50);
        
        assertThat(cache1.size()).isEqualTo(1);
        assertThat(cache2.size()).isEqualTo(1);
    }
    
    @Test
    public void testInvalidationOnRemove() throws InterruptedException {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5).invalidateEntryOnChange(true);
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        
        RLocalCachedMap<String, Integer> map2 = redisson.getLocalCachedMap("test", options);
        Cache<CacheKey, CacheValue> cache2 = Deencapsulation.getField(map2, "cache");
        
        map1.put("1", 1);
        map1.put("2", 2);
        
        assertThat(map2.get("1")).isEqualTo(1);
        assertThat(map2.get("2")).isEqualTo(2);
        
        assertThat(cache1.size()).isEqualTo(2);
        assertThat(cache2.size()).isEqualTo(2);

        map1.remove("1");
        map2.remove("2");
        Thread.sleep(50);
        
        assertThat(cache1.size()).isEqualTo(0);
        assertThat(cache2.size()).isEqualTo(0);
    }
    
    @Test
    public void testLFU() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LFU).cacheSize(5));
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");

        map.put("12", 1);
        map.put("14", 2);
        map.put("15", 3);
        map.put("16", 4);
        map.put("17", 5);
        map.put("18", 6);
        
        assertThat(cache.size()).isEqualTo(5);
        assertThat(map.size()).isEqualTo(6);
        assertThat(map.keySet()).containsOnly("12", "14", "15", "16", "17", "18");
        assertThat(map.values()).containsOnly(1, 2, 3, 4, 5, 6);
    }
    
    @Test
    public void testLRU() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults().evictionPolicy(EvictionPolicy.LRU).cacheSize(5));
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");

        map.put("12", 1);
        map.put("14", 2);
        map.put("15", 3);
        map.put("16", 4);
        map.put("17", 5);
        map.put("18", 6);
        
        assertThat(cache.size()).isEqualTo(5);
        assertThat(map.size()).isEqualTo(6);
        assertThat(map.keySet()).containsOnly("12", "14", "15", "16", "17", "18");
        assertThat(map.values()).containsOnly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testSize() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");

        map.put("12", 1);
        map.put("14", 2);
        map.put("15", 3);
        
        assertThat(cache.size()).isEqualTo(3);
        assertThat(map.size()).isEqualTo(3);
    }

    
    @Test
    public void testPut() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");

        map.put("12", 1);
        map.put("14", 2);
        map.put("15", 3);

        assertThat(cache).containsValues(new CacheValue("12", 1), new CacheValue("12", 2), new CacheValue("15", 3));
        assertThat(map.get("12")).isEqualTo(1);
        assertThat(map.get("14")).isEqualTo(2);
        assertThat(map.get("15")).isEqualTo(3);
        
        RLocalCachedMap<String, Integer> map1 = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        
        assertThat(map1.get("12")).isEqualTo(1);
        assertThat(map1.get("14")).isEqualTo(2);
        assertThat(map1.get("15")).isEqualTo(3);
    }
    
    @Test
    public void testGetAll() {
        RMap<String, Integer> map = redisson.getLocalCachedMap("getAll", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put("1", 100);
        map.put("2", 200);
        map.put("3", 300);
        map.put("4", 400);

        assertThat(cache.size()).isEqualTo(4);
        Map<String, Integer> filtered = map.getAll(new HashSet<String>(Arrays.asList("2", "3", "5")));

        Map<String, Integer> expectedMap = new HashMap<String, Integer>();
        expectedMap.put("2", 200);
        expectedMap.put("3", 300);
        assertThat(filtered).isEqualTo(expectedMap);
        
        RMap<String, Integer> map1 = redisson.getLocalCachedMap("getAll", LocalCachedMapOptions.defaults());
        
        Map<String, Integer> filtered1 = map1.getAll(new HashSet<String>(Arrays.asList("2", "3", "5")));

        assertThat(filtered1).isEqualTo(expectedMap);
    }

    @Test
    public void testPutAll() {
        Map<Integer, String> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Map<Integer, String> map1 = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        Cache<CacheKey, CacheValue> cache1 = Deencapsulation.getField(map1, "cache");
        map.put(1, "1");
        map.put(2, "2");
        map.put(3, "3");

        Map<Integer, String> joinMap = new HashMap<Integer, String>();
        joinMap.put(4, "4");
        joinMap.put(5, "5");
        joinMap.put(6, "6");
        map.putAll(joinMap);

        assertThat(cache.size()).isEqualTo(6);
        assertThat(cache1.size()).isEqualTo(0);
        assertThat(map.keySet()).containsOnly(1, 2, 3, 4, 5, 6);
        
        map1.putAll(joinMap);
        
        assertThat(cache.size()).isEqualTo(3);
        assertThat(cache1.size()).isEqualTo(3);
    }
    
    @Test
    public void testAddAndGet() throws InterruptedException {
        RMap<Integer, Integer> map = redisson.getLocalCachedMap("getAll", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(1, 100);

        Integer res = map.addAndGet(1, 12);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(res).isEqualTo(112);
        res = map.get(1);
        assertThat(res).isEqualTo(112);

        RMap<Integer, Double> map2 = redisson.getLocalCachedMap("getAll2", LocalCachedMapOptions.defaults());
        map2.put(1, new Double(100.2));

        Double res2 = map2.addAndGet(1, new Double(12.1));
        assertThat(res2).isEqualTo(112.3);
        res2 = map2.get(1);
        assertThat(res2).isEqualTo(112.3);

        RMap<String, Integer> mapStr = redisson.getLocalCachedMap("mapStr", LocalCachedMapOptions.defaults());
        assertThat(mapStr.put("1", 100)).isNull();

        assertThat(mapStr.addAndGet("1", 12)).isEqualTo(112);
        assertThat(mapStr.get("1")).isEqualTo(112);
        assertThat(cache.size()).isEqualTo(1);
    }
    
    @Test
    public void testFastPutIfAbsent() throws Exception {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        
        SimpleKey key = new SimpleKey("1");
        SimpleValue value = new SimpleValue("2");
        map.put(key, value);
        assertThat(map.fastPutIfAbsent(key, new SimpleValue("3"))).isFalse();
        assertThat(cache.size()).isEqualTo(1);
        assertThat(map.get(key)).isEqualTo(value);

        SimpleKey key1 = new SimpleKey("2");
        SimpleValue value1 = new SimpleValue("4");
        assertThat(map.fastPutIfAbsent(key1, value1)).isTrue();
        assertThat(cache.size()).isEqualTo(2);
        assertThat(map.get(key1)).isEqualTo(value1);
    }
    
    @Test
    public void testReadAllEntrySet() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple12", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));
        map.put(new SimpleKey("33"), new SimpleValue("44"));
        map.put(new SimpleKey("5"), new SimpleValue("6"));

        assertThat(map.readAllEntrySet().size()).isEqualTo(3);
        assertThat(cache.size()).isEqualTo(3);
        Map<SimpleKey, SimpleValue> testMap = new HashMap<>(map);
        assertThat(map.readAllEntrySet()).containsOnlyElementsOf(testMap.entrySet());
        
        RMap<SimpleKey, SimpleValue> map2 = redisson.getLocalCachedMap("simple12", LocalCachedMapOptions.defaults());
        assertThat(map2.readAllEntrySet()).containsOnlyElementsOf(testMap.entrySet());
    }
    
    @Test
    public void testPutIfAbsent() throws Exception {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple12", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");

        SimpleKey key = new SimpleKey("1");
        SimpleValue value = new SimpleValue("2");
        map.put(key, value);
        Assert.assertEquals(value, map.putIfAbsent(key, new SimpleValue("3")));
        Assert.assertEquals(value, map.get(key));

        SimpleKey key1 = new SimpleKey("2");
        SimpleValue value1 = new SimpleValue("4");
        Assert.assertNull(map.putIfAbsent(key1, value1));
        Assert.assertEquals(value1, map.get(key1));
        assertThat(cache.size()).isEqualTo(2);
    }
    
    @Test
    public void testRemoveValue() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple12", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));

        boolean res = map.remove(new SimpleKey("1"), new SimpleValue("2"));
        Assert.assertTrue(res);

        SimpleValue val1 = map.get(new SimpleKey("1"));
        Assert.assertNull(val1);

        Assert.assertEquals(0, map.size());
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    public void testRemoveValueFail() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple12", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));

        boolean res = map.remove(new SimpleKey("2"), new SimpleValue("1"));
        Assert.assertFalse(res);

        boolean res1 = map.remove(new SimpleKey("1"), new SimpleValue("3"));
        Assert.assertFalse(res1);

        SimpleValue val1 = map.get(new SimpleKey("1"));
        Assert.assertEquals("2", val1.getValue());
        assertThat(cache.size()).isEqualTo(1);
    }
    
    @Test
    public void testReplaceOldValueFail() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));

        boolean res = map.replace(new SimpleKey("1"), new SimpleValue("43"), new SimpleValue("31"));
        Assert.assertFalse(res);

        SimpleValue val1 = map.get(new SimpleKey("1"));
        Assert.assertEquals("2", val1.getValue());
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    public void testReplaceOldValueSuccess() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));

        boolean res = map.replace(new SimpleKey("1"), new SimpleValue("2"), new SimpleValue("3"));
        Assert.assertTrue(res);

        boolean res1 = map.replace(new SimpleKey("1"), new SimpleValue("2"), new SimpleValue("3"));
        Assert.assertFalse(res1);

        SimpleValue val1 = map.get(new SimpleKey("1"));
        Assert.assertEquals("3", val1.getValue());
        assertThat(cache.size()).isEqualTo(1);
    }
    
    @Test
    public void testReplaceValue() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(new SimpleKey("1"), new SimpleValue("2"));

        SimpleValue res = map.replace(new SimpleKey("1"), new SimpleValue("3"));
        Assert.assertEquals("2", res.getValue());
        assertThat(cache.size()).isEqualTo(1);

        SimpleValue val1 = map.get(new SimpleKey("1"));
        Assert.assertEquals("3", val1.getValue());
    }
    
    @Test
    public void testReadAllValues() {
        RMap<SimpleKey, SimpleValue> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        
        map.put(new SimpleKey("1"), new SimpleValue("2"));
        map.put(new SimpleKey("33"), new SimpleValue("44"));
        map.put(new SimpleKey("5"), new SimpleValue("6"));
        assertThat(cache.size()).isEqualTo(3);

        assertThat(map.readAllValues().size()).isEqualTo(3);
        Map<SimpleKey, SimpleValue> testMap = new HashMap<>(map);
        assertThat(map.readAllValues()).containsOnlyElementsOf(testMap.values());
        
        RMap<SimpleKey, SimpleValue> map2 = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        assertThat(map2.readAllValues()).containsOnlyElementsOf(testMap.values());
    }

    
    @Test
    public void testFastRemoveAsync() throws InterruptedException, ExecutionException {
        RMap<Integer, Integer> map = redisson.getLocalCachedMap("simple", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put(1, 3);
        map.put(3, 5);
        map.put(4, 6);
        map.put(7, 8);

        assertThat(map.fastRemoveAsync(1, 3, 7).get()).isEqualTo(3);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    public void testRemove() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        Cache<CacheKey, CacheValue> cache = Deencapsulation.getField(map, "cache");
        map.put("12", 1);

        assertThat(cache.size()).isEqualTo(1);
        
        assertThat(map.remove("12")).isEqualTo(1);
        
        assertThat(cache.size()).isEqualTo(0);
        
        assertThat(map.remove("14")).isNull();
    }

    @Test
    public void testFastRemove() throws InterruptedException, ExecutionException {
        RLocalCachedMap<Integer, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        map.put(1, 3);
        map.put(2, 4);
        map.put(7, 8);

        assertThat(map.fastRemove(1, 2)).isEqualTo(2);
        assertThat(map.fastRemove(2)).isEqualTo(0);
        assertThat(map.size()).isEqualTo(1);
    }

    @Test
    public void testFastPut() {
        RLocalCachedMap<String, Integer> map = redisson.getLocalCachedMap("test", LocalCachedMapOptions.defaults());
        Assert.assertTrue(map.fastPut("1", 2));
        assertThat(map.get("1")).isEqualTo(2);
        Assert.assertFalse(map.fastPut("1", 3));
        assertThat(map.get("1")).isEqualTo(3);
        Assert.assertEquals(1, map.size());
    }

    
}
