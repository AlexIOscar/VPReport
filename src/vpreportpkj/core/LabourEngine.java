package vpreportpkj.core;

import vpreportpkj.core.labrepo.LabourRepository;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LabourEngine {

    //путь, с файлом по которому ассоциирован быстрый репозиторий
    private final String pathFastRepo;
    private final String pathRepo;
    private Map<String, CyclicStorage<Integer>> fastRepo;
    private LabourRepository repository;

    //LabEngine нельзя создать напрямую из клиентского кода
    private LabourEngine(String pathFastRepo, String pathRepo) {
        this.pathFastRepo = pathFastRepo;
        this.pathRepo = pathRepo;
    }

    //сохранить репозиторий
    private void saveRepo() {
        try (FileOutputStream fos = new FileOutputStream(pathRepo);
             ObjectOutputStream oos = new ObjectOutputStream(fos);
             FileLock lock = fos.getChannel().lock(0L, Long.MAX_VALUE, true)
        ) {
            oos.writeObject(repository);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    //загрузить репозиторий
    private LabourRepository loadRepo() throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(pathRepo);
             FileLock lock = fis.getChannel().lock(0L, Long.MAX_VALUE, true);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (LabourRepository) ois.readObject();
        } catch (IOException ioex) {
            System.out.println("IO Exception, file is busy or doesn't exist");
            throw new IOException("IO Exception, file is busy or doesn't exist", ioex);
        } catch (ClassNotFoundException cnfex) {
            System.out.println("Repository reading failure, the file may be corrupted");
            throw new ClassNotFoundException("Repository reading failure, the file may be corrupted", cnfex);
        }
    }

    private static LabourEngine getInstance(String pathRepo) {
        LabourEngine out = new LabourEngine(null, pathRepo);
        try {
            out.fastRepo = out.pullCyclicRepo();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage() + ", the empty repo will be returned");
            out.fastRepo = new HashMap<>();
        } catch (IOException ioex) {
            System.out.println(ioex.getMessage() + ", the empty repo will be returned");
            out.fastRepo = new HashMap<>();
        }
        return null;
    }

    //TODO если файл окажется изменен другим процессом во время попытки pullCyclicRepo, то произойдет исключение, и
    // будет создан пустой репозиторий, который перезапишет существующий в конце сеанса. Это исключает возможность
    // работы нескольких клиентов с сетевым общим репозиторием в такой схеме
    public static LabourEngine getFastEngine(String pathFastRepo) {
        LabourEngine out = new LabourEngine(pathFastRepo, null);
        //полный (fullRepo) репозиторий при такой конфигурации остается пустым
        try {
            out.fastRepo = out.pullCyclicRepo();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage() + ", the empty repo will be returned");
            out.fastRepo = new HashMap<>();
        } catch (IOException ioex) {
            System.out.println(ioex.getMessage() + ", the empty repo will be returned");
            out.fastRepo = new HashMap<>();
        }
        return out;
    }

    //подтянуть быстрый репозиторий из ассоциированного файла
    private Map<String, CyclicStorage<Integer>> pullCyclicRepo() throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(pathFastRepo);
             FileLock lock = fis.getChannel().lock(0L, Long.MAX_VALUE, true);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Set<Integer> s = (Set<Integer>) ois.readObject();
            Integer i = (Integer) ois.readObject();
            return (Map) ois.readObject();
        } catch (IOException ioex) {
            System.out.println("IO Exception, file is busy or doesn't exist");
            throw new IOException("IO Exception, file is busy or doesn't exist", ioex);
        } catch (ClassNotFoundException cnfex) {
            System.out.println("Repository reading failure, the file may be corrupted");
            throw new ClassNotFoundException("Repository reading failure, the file may be corrupted", cnfex);
        }
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
        return (st.mark + '$' + st.position).toUpperCase().replaceAll("\\s+| +", "");
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

    //обновить быстрый репозиторий "брутфорсом", без проверок, всеми значениями
    private void updateFastRepo(List<SingleTuple> inputList) {
        inputList.forEach(this::pushLabToFast);
    }

    //получить экспертное время из репозитория по ключу
    private int getExpertTime(SingleTuple st) {
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
     *
     * @param st           проверяемый кортеж
     * @param filterFactor фильтрующая кратность превышения/занижения времени относительно репозитория
     * @param maxVal       пороговое значение проверки
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
     * Проверяет время операции. Проверяет каждый кортеж на соответствие времени репозитория. Если время не
     * соответствует ожидаемому, т.е. является "подозрительно большим" (см. FilterFactor), то будет возвращено время
     * из репозитория на замену. В противоположном случае, возвращается время из входящего кортежа. Если флаг
     * обновления (update) активен, то любое не "подозрительное" время будет помещено в репозиторий.
     * Особенностью этой реализации является то, что в качестве первого времени в новый storage репозитория будет
     * помещено любое сколь угодно большое время, но при повторных записях валидного малого времени его влияние на
     * среднюю величину постепенно будет сведено на нет, пока в конце концов оно не будет вытеснено за "дальнюю"
     * границу storage
     *
     * @param st           проверяемый кортеж
     * @param filterFactor если время-кандидат превышает среднее по репозиторию более чем в filterFactor раз, то оно
     *                     будет отклонено, а вместо него возвращено время из репозитория
     * @param update       если true, то заносить значение в репозиторий, если false - то не изменять репозиторий
     * @return экспертное время
     */
    public int chkTimeAdv(SingleTuple st, int filterFactor, boolean update) {
        int exTime = getExpertTime(st);
        if (exTime != -1 && st.duration / exTime > filterFactor) {
            //если данные от репозитория есть, а значение-кандидат отличается от него сверх предела вверх
            System.out.print("expert time applied for " + st.mark + " " + st.position + " ");
            System.out.println(exTime + " instead " + st.duration);
            return exTime;
        }
        if (update) {
            pushLabToFast(st);
        }
        return st.duration;
    }

    public Map<String, CyclicStorage<Integer>> getFastRepo() {
        return fastRepo;
    }

    /**
     * Специальный класс, представляющий собой замкнутый ограниченный склад объектов типа T. В него может быть
     * помещено новое значение, при этом самое устаревшее значение будет удалено с "дальнего" конца
     *
     * @param <T> тип хранимых объектов
     */
    public static class CyclicStorage<T> implements Serializable {
        private static final long serialVersionUID = 2L;
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

        public int getPointer() {
            return pointer;
        }

    }
}
