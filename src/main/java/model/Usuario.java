package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// representa o usuário do sistema, o pobre coitado que tá logado
public class Usuario {
    private String numero;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    // seta o número do usuário e avisa geral que mudou o numero
    public void setNumero(String numero) {
        String antigo = this.numero;
        this.numero = numero;
        support.firePropertyChange("numero", antigo, numero);
    }

    public String getNumero() {
        return numero;
    }

    // adiciona um listener pra quando alguma coisa mudar
    public void addListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }
}
