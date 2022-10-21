package vpreportpkj.core.labrepo;

import vpreportpkj.core.SingleTuple;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static vpreportpkj.core.LabourEngine.createKey;

/*
Вытащено из LabourEngine в процессе рефакторинга на вынесение репозиториев в специальный интерфейс. Все равно этот
тип репозитория не используется ввиду тяжеловесности хранения всей истории операций в памяти, и сериализации/
десериализации всего этого добра всякий раз при загрузке или останове программы. По этой же причине сигнатуры не
приведены в соответствие с интерфейсом LabourRepository (метод получения экспертной величины Labour никогда здесь не
был реализован)
 */

public class FullRepo {
    private Map<String, Map<Long, Integer>> fullRepo;
    private final String pathFullRepo;

    public FullRepo(String pathFullRepo) {
        this.pathFullRepo = pathFullRepo;
    }

    private boolean pushLabour(SingleTuple st) {
        Long time = st.getCompleteTime().getTime() / 1000;
        String mainKey = createKey(st);
        Map<Long, Integer> innerMap = fullRepo.get(mainKey);
        if (innerMap == null) {
            Map<Long, Integer> newInner = new HashMap<>();
            newInner.put(time, st.getDuration());
            fullRepo.put(mainKey, newInner);
            return true;
        }

        if (!innerMap.containsKey(time)) {
            innerMap.put(time, st.getDuration());
            return true;
        }
        return false;
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
            System.out.println("The fast repo associated file is corrupted");
            throw ex;
        }
    }

    public Map<String, Map<Long, Integer>> getFullRepo() {
        return fullRepo;
    }
}
