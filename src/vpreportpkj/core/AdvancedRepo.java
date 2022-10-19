package vpreportpkj.core;

import vpreportpkj.core.LabourEngine.CyclicStorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static vpreportpkj.core.LabourEngine.createKey;

public class AdvancedRepo implements Serializable {
    private static final long serialVersionUID = 1L;
    final int rowCapacity;
    final private Map<String, AdvRepoFacility> repo;


    public AdvancedRepo(int capacity) {
        this.rowCapacity = capacity;
        this.repo = new HashMap<>();
    }

    public void push(SingleTuple st) {
        String mainKey = createKey(st);
        long minorKey = st.completeTime.getTime() / 1000L;
        int duration = st.duration;
        AdvRepoFacility arf = repo.get(mainKey);
        //если по такому главному ключу ничего нет, создаем новый ARE, куда складываем значения
        if (arf == null) {
            AdvRepoFacility fac = new AdvRepoFacility(rowCapacity);
            fac.storage.put(minorKey, duration);
            fac.cyclic.push(minorKey);
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

    static class AdvRepoFacility {
        private final CyclicStorage<Long> cyclic;
        private final Map<Long, Integer> storage;

        public AdvRepoFacility(int capacity) {
            cyclic = new CyclicStorage<>(new Long[capacity]);
            storage = new HashMap<>(capacity);
        }
    }
}
