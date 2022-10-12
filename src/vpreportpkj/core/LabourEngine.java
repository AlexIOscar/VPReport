package vpreportpkj.core;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LabourEngine {
    static Map<String, Map<Long, Integer>> labourRepo = new HashMap<>();
    static String path = "C:\\Users\\Tolstokulakov_AV\\VPRP\\pcRepo.dat";
    private final Map<String, CyclicStorage<Integer>> fastRepo;

    public LabourEngine(Map<String, CyclicStorage<Integer>> fastRepo) {
        this.fastRepo = fastRepo;
    }

    /*
    static {
        pullRepo(path);
    }
     */

    static boolean pushLabour(SingleTuple st) {
        Long time = st.completeTime.getTime() / 1000;
        String mainKey = createKey(st);
        Map<Long, Integer> innerMap = labourRepo.get(mainKey);
        if (innerMap == null) {
            Map<Long, Integer> newInner = new HashMap<>();
            newInner.put(time, st.duration);
            labourRepo.put(mainKey, newInner);
            return true;
        }

        if (!innerMap.containsKey(time)) {
            innerMap.put(time, st.duration);
            return true;
        }
        return false;
    }

    public void pushLabToCyclic(SingleTuple st) {
        String mainKey = createKey(st);
        CyclicStorage<Integer> storage = fastRepo.get(mainKey);
        if (storage == null) {
            storage = new CyclicStorage<>(new Integer[20]);
            fastRepo.put(mainKey, storage);
        }
        storage.push(st.duration);
    }

    private static String createKey(SingleTuple st) {
        return (st.mark + st.position).replaceAll("\\s+| +", "");
    }

    static void pushRepo(String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(path)))) {
            oos.writeObject(labourRepo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    static void pullRepo(String path) {
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(path)))) {
            labourRepo = (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    static void pushCyclicRepo(String path, Map<String, CyclicStorage<Integer>> repo) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(path)))) {
            oos.writeObject(repo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    static Map<String, CyclicStorage<Integer>> pullCyclicRepo(String path) {
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(path)))) {
            return (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public static Map<String, Map<Long, Integer>> getLabourRepo() {
        return labourRepo;
    }

    public static class CyclicStorage<T> implements Serializable {
        private final T[] storage;
        private int pointer = 0;
        private final int capacity;

        public CyclicStorage(T[] storage) {
            this.capacity = storage.length;
            this.storage = storage;
        }

        public void push (T value) {
            storage[pointer] = value;
            pointer++;
            pointer = pointer % capacity;
        }

        public T[] getStorage() {
            return storage;
        }
    }
}
