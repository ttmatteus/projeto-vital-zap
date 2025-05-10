package observer;

import java.util.ArrayList;
import java.util.List;

public class Observable {
    private final List<Observer> observadores = new ArrayList<>();

    public void adicionarObserver(Observer o) {
        observadores.add(o);
    }

    public void notificarObservers(String acao) {
        for (Observer o : observadores) {
            o.atualizar(acao);
        }
    }
}
