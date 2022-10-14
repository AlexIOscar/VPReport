package vpreportpkj.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LabourEngine {
    private Map<String, Map<Long, Integer>> fullRepo;
    private final String pathFullRepo;

    //путь, с файлом по которому ассоциирован быстрый репозиторий
    private final String pathFastRepo;
    private Map<String, CyclicStorage<Integer>> fastRepo;

    //LabEngine нельзя создать напрямую из клиентского кода
    private LabourEngine(String pathFullRepo, String pathFastRepo) {
        this.pathFullRepo = pathFullRepo;
        this.pathFastRepo = pathFastRepo;
    }

    public static LabourEngine getFastEngine(String pathFastRepo) {
        LabourEngine out = new LabourEngine(null, pathFastRepo);
        //полный репозиторий при такой конфигурации остается пустым
        out.fullRepo = new HashMap<>();
        try {
            out.fastRepo = out.pullCyclicRepo();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("The fast repo associated file corrupted or doesn't exist, the empty repo will be " +
                    "returned");
            out.fastRepo = new HashMap<>();
        }
        return out;
    }

    private boolean pushLabour(SingleTuple st) {
        Long time = st.completeTime.getTime() / 1000;
        String mainKey = createKey(st);
        Map<Long, Integer> innerMap = fullRepo.get(mainKey);
        if (innerMap == null) {
            Map<Long, Integer> newInner = new HashMap<>();
            newInner.put(time, st.duration);
            fullRepo.put(mainKey, newInner);
            return true;
        }

        if (!innerMap.containsKey(time)) {
            innerMap.put(time, st.duration);
            return true;
        }
        return false;
    }

    private void pushLabToFast(SingleTuple st) {
        String mainKey = createKey(st);
        CyclicStorage<Integer> storage = fastRepo.get(mainKey);
        if (storage == null) {
            storage = new CyclicStorage<>(new Integer[30]);
            fastRepo.put(mainKey, storage);
        }
        storage.push(st.duration);
    }

    public static String createKey(SingleTuple st) {
        return (st.mark + st.position).replaceAll("\\s+| +", "");
    }

    private void pushRepo() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(pathFullRepo)))) {
            oos.writeObject(fullRepo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private Map<String, Map<Long, Integer>> pullRepo() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(pathFullRepo)))) {
            return (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println("FastRepo pulling error, the empty repo will be created");
            throw ex;
        }
    }

    /**
     * записать быстрый репозиторий в ассоциированный файл
     */
    public void pushFastRepo() {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(pathFastRepo)))) {
            oos.writeObject(fastRepo);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    //подтянуть быстрый репозиторий из ассоциированного файла
    private Map<String, CyclicStorage<Integer>> pullCyclicRepo() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream
                (Files.newInputStream(Paths.get(pathFastRepo)))) {
            return (Map) ois.readObject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    //обновить быстрый репозиторий "брутфорсом", без проверок, всеми значениями
    private void updateFastRepo(List<SingleTuple> inputList) {
        inputList.forEach(this::pushLabToFast);
    }

    //получить экспертное время из репозитория по ключу
    public int getExpertTime(SingleTuple st) {
        String key = createKey(st);
        CyclicStorage<Integer> cst = fastRepo.get(key);
        Integer[] storage;
        if (cst != null) {
            storage = cst.getStorage();
        } else {
            //клиентский код должен рассматривать -1 как отсутствие результата
            return -1;
        }
        AtomicInteger notZeroCases = new AtomicInteger();
        int sum =
                Arrays.stream(storage).filter(Objects::nonNull).peek(t -> notZeroCases.getAndIncrement()).mapToInt(i -> i).sum();
        if (notZeroCases.get() > 0) {
            return sum / notZeroCases.get();
        } else {
            return -1;
        }
    }

    /**
     * Проверяет время операции. Вариант, проверяющий только время выше подозрительного уровня.
     * Любое время не выше подозрительного уровня maxVal напрямую идет в репозиторий.
     * Если время превышает подозрительный уровень, оно сравнивается с репозиторием, и если только проходит проверку
     * на соответствие данным репозитория (не выше и не ниже ожидаемого), пушится внутрь него на хранение.
     * @param st проверяемый кортеж
     * @param filterFactor фильтрующая кратность превышения/занижения времени относительно репозитория
     * @param maxVal пороговое значение проверки
     * @param defaultValue значение по умолчанию, применяемое если в репозитории ничего нет, а время подозрительное
     * @return экспертное время
     */
    public int chkTimeWithUpd(SingleTuple st, int filterFactor, int maxVal, int defaultValue) {
        int exTime = getExpertTime(st);
        //если длительность свыше подозрительной
        if (st.duration > maxVal) {
            //если нет данных от репозитория, возвращаем значение по умолчанию
            if (exTime == -1) {
                return defaultValue;
            }
            //если данные от репозитория есть, а значение-кандидат отличается от него сверх предела, возвращаем
            // значение из репозитория
            if ((st.duration / exTime > filterFactor || exTime / st.duration > filterFactor)) {
                System.out.print("expert time applied for " + st.mark + " " + st.position + " ");
                System.out.println(exTime + " instead " + st.duration);
                return exTime;
            }
        }
        pushLabToFast(st);
        return st.duration;
    }


    /**
     * Проверяет время операции. Вариант, проверяющий каждый кортеж на соответствие времени репозитория. Только если
     * время не соответствует ожидаемому, оно будет отклонено. В остальных случаях - помещено в репозиторий.
     * Особенностью этой реализации является то, что в качестве первого времени в новый storage репозитория будет
     * помещено любое сколь угодно большое время, но при повторных записях валидного малого времени его влияние на
     * среднюю величину постепенно будет сведено на нет, пока в конце концов оно не будет вытеснено за "дальнюю"
     * границу storage
     * @param st проверяемый кортеж
     * @param filterFactor если время-кандидат превышает среднее по репозиторию более чем в filterFactor раз, то оно
     *                     будет отклонено, а вместо него возвращено время из репозитория
     * @return экспертное время
     */
    public int chkTWAAdv(SingleTuple st, int filterFactor) {
        int exTime = getExpertTime(st);
        if (exTime != -1) {
            //если данные от репозитория есть, а значение-кандидат отличается от него сверх предела вверх
            if (st.duration / exTime > filterFactor) {
                System.out.print("expert time applied for " + st.mark + " " + st.position + " ");
                System.out.println(exTime + " instead " + st.duration);
                return exTime;
            }
        }
        pushLabToFast(st);
        return st.duration;
    }

    public Map<String, Map<Long, Integer>> getFullRepo() {
        return fullRepo;
    }

    public Map<String, CyclicStorage<Integer>> getFastRepo() {
        return fastRepo;
    }

    /**
     * Специальный класс, представляющий собой замкнутый ограниченный склад объектов типа T. В него может быть
     * помещено новое значение, при этом самое устаревшее значение будет удалено с "дальнего" конца
     * @param <T> тип хранимых объектов
     */
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