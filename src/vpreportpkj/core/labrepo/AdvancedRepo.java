package vpreportpkj.core.labrepo;

import vpreportpkj.core.LabourEngine.CyclicStorage;
import vpreportpkj.core.SingleTuple;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static vpreportpkj.core.LabourEngine.createKey;

public class AdvancedRepo implements Serializable, LabourRepository {
    private static final long serialVersionUID = 1L;
    private final int rowCapacity;
    final private Map<String, AdvRepoFacility> repo;
    transient private double filterFactor;
    transient private boolean update;
    transient StringBuilder sb = new StringBuilder();

    public AdvancedRepo(int capacity, double filterFactor, boolean update) {
        this.rowCapacity = capacity;
        this.repo = new HashMap<>();
        this.filterFactor = filterFactor;
        this.update = update;
    }

    private void push(SingleTuple st) {
        String mainKey = createKey(st);
        long minorKey = st.getCompleteTime().getTime() / 1000L;
        int duration = st.getDuration();
        AdvRepoFacility arf = repo.get(mainKey);
        //если по такому главному ключу ничего нет, создаем новый ARE, куда складываем значения
        if (arf == null) {
            AdvRepoFacility fac = new AdvRepoFacility(rowCapacity);
            fac.storage.put(minorKey, duration);
            fac.cyclic.push(minorKey);
            repo.put(mainKey, fac);
            return;
        }
        //если по такому времени (минорному ключу) уже есть значение, просто выходим
        if (arf.storage.get(minorKey) != null) {
            return;
        }
        //получаем пойнтер, на который станет указывать cyclic после следующего push
        int nextPtr = (arf.cyclic.getPointer() + 1) % arf.cyclic.getStorage().length;
        //удаляем из склада запись по ключу, извлеченному из cyclic по ключу, который будет затерт после push
        arf.storage.remove(arf.cyclic.getStorage()[nextPtr]);
        //помещаем в склад по новому ключу новое значение duration
        arf.storage.put(minorKey, duration);
        //и пушим новый ключ в cyclic
        arf.cyclic.push(minorKey);
    }

    @Override
    public int chkTime(SingleTuple st) {
        return chkTime(st, update);
    }

    //перегруженный вариант, предназначенный для вызова с конкретным параметром update (что позволяет обходить
    // значение, определенное классом)
    public int chkTime(SingleTuple st, boolean update) {
        int exTime = getExpertTime(st);
        if (exTime != -1 && exTime != 0 && (double) st.getDuration() / exTime > filterFactor) {
            //если данные от репозитория есть, а значение-кандидат отличается от него сверх предела вверх
            /*
            sb.append("expert time applied for ").append(st.getMark()).append(" ").append(st.getPosition()).append(" ");
            sb.append(exTime).append(" instead ").append(st.getDuration()).append('\n');
             */
            return exTime;
        }
        if (update) {
            push(st);
        }
        return st.getDuration();
    }

    //Получить среднее время из репозитория. -1, если по данному ключу данных нет
    private int getExpertTime(SingleTuple st) {
        String key = createKey(st);
        AdvRepoFacility facility = repo.get(key);
        Map<Long, Integer> storage;
        if (facility != null) {
            storage = facility.getStorage();
        } else {
            //клиентский код должен рассматривать -1 как отсутствие результата
            return -1;
        }
        if (storage.isEmpty()) {
            return -1;
        }
        /*
        int sum = storage.values().stream().mapToInt(i -> i).sum();
        return sum / storage.size();
         */
        //медианное значение
        Integer[] sorted = storage.values().toArray(new Integer[0]);
        Arrays.sort(sorted);
        return sorted[sorted.length / 2 - 1 + sorted.length % 2];
    }

    @Override
    public StringBuilder getStringBuilder() {
        return sb;
    }

    public void setFilterFactor(double filterFactor) {
        this.filterFactor = filterFactor;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void setSb(StringBuilder sb) {
        this.sb = sb;
    }

    //Класс, объединяющий в пару карту и циклический "счетчик". Поскольку карта не ограничена и не имеет отношения
    // порядка, счетчик служит для того, чтобы знать (и удалять) самое старое значение в карты. Посредством этого
    //механизма обеспечивается нерасширяемость карты свыше фиксированного количества хранимых пар key-value
    static class AdvRepoFacility implements Serializable {
        private static final long serialVersionUID = 1L;
        private final CyclicStorage<Long> cyclic;
        private final Map<Long, Integer> storage;

        public AdvRepoFacility(int capacity) {
            cyclic = new CyclicStorage<>(new Long[capacity]);
            storage = new HashMap<>(capacity);
        }

        public CyclicStorage<Long> getCyclic() {
            return cyclic;
        }

        public Map<Long, Integer> getStorage() {
            return storage;
        }
    }
}
