package vpreportpkj.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LabourEngine {
    private Map<String, Map<Long, Integer>> labourRepo;
    private final String pathRepo;
    private final String pathFastRepo;
    private Map<String, CyclicStorage<Integer>> fastRepo;

    public LabourEngine(String pathRepo, String pathFastRepo) {
        this.pathRepo = pathRepo;
        this.pathFastRepo = pathFastRepo;
    }

    public boolean pushLabour(SingleTuple st) {
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

    public static String createKey(SingleTuple st) {
        return (st.mark + st.position).replaceAll("\\s+| +", "");
    }

    public void pushRepo() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(pathRepo)))) {
            oos.writeObject(labourRepo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public Map<String, Map<Long, Integer>> pullRepo() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(pathRepo)))) {
            return (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    public void pushCyclicRepo() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(pathFastRepo)))) {
            oos.writeObject(fastRepo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public Map<String, CyclicStorage<Integer>> pullCyclicRepo() throws IOException, ClassNotFoundException {
        long length = new File(pathFastRepo).length();
        if (length == 0) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(pathFastRepo)))) {
            return (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Map<Long, Integer>> getLabourRepo() {
        return labourRepo;
    }

    public Map<String, CyclicStorage<Integer>> getFastRepo() {
        return fastRepo;
    }

    public static class CyclicStorage<T> implements Serializable {
        private final T[] storage;
        private int pointer = 0;
        private final int capacity;

        public CyclicStorage(T[] storage) {
            this.capacity = storage.length;
            this.storage = storage;
        }

        public void push(T value) {
            storage[pointer] = value;
            pointer++;
            pointer = pointer % capacity;
        }

        public T[] getStorage() {
            return storage;
        }
    }
}
