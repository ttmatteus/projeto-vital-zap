package model;

import java.time.LocalTime;

// classe que representa uma mensagem nesse de sistema de chat muito lindinho e bonitinho
public class Mensagem {
    private final String texto;
    private final String remetente;
    private LocalTime horario;
    private boolean lida;
    private final String chatName;

    // construtor padrão, pega a hora atual e mete bronca
    public Mensagem(String texto, String remetente, String chatName) {
        this.texto = texto;
        this.remetente = remetente;
        this.horario = LocalTime.now();
        this.lida = false;
        this.chatName = chatName;
    }

    // construtor completo,
    public Mensagem(String texto, String remetente, LocalTime horario, boolean lida, String chatName) {
        this.texto = texto;
        this.remetente = remetente;
        this.horario = horario;
        this.lida = lida;
        this.chatName = chatName;
    }

    // pega o texto da msg
    public String getTexto() {
        return texto;
    }

    // pega quem mandou
    public String getRemetente() {
        return remetente;
    }

    // pega a hora em que foi enviado
    public LocalTime getHorario() {
        return horario;
    }
}
