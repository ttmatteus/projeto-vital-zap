package view;

import interfaces.Pessoa;
import observer.Observer;

public class UsuarioChat implements Pessoa, Observer {
    private final String nome;
    private boolean permissaoEnviada;
    private int armazenamentoUsado; // Em MB
    private int dadosUsados; // Em MB
    private boolean visualizouMensagem;

    public UsuarioChat(String nome) {
        this.nome = nome;
        this.permissaoEnviada = false;
        this.armazenamentoUsado = 0;
        this.dadosUsados = 0;
        this.visualizouMensagem = false;
    }

    public String getNome() {
        return nome;
    }

    public boolean temPermissao() {
        return permissaoEnviada;
    }

    public int getArmazenamentoUsado() {
        return armazenamentoUsado;
    }

    public int getDadosUsados() {
        return dadosUsados;
    }

    public boolean isVisualizouMensagem() {
        return visualizouMensagem;
    }

    @Override
    public void visto() {
        this.visualizouMensagem = true;
    }

    @Override
    public void permite() {
        this.permissaoEnviada = true;
    }

    @Override
    public void armazena() {
        this.armazenamentoUsado += 1; // Simula 1 MB por mensagem
    }

    @Override
    public void usaDados() {
        this.dadosUsados += 1; // Simula 1 MB de dados por mensagem
    }

    @Override
    public void atualizar(String acao) {
        if (acao.contains("Nova mensagem") && !nome.equals("You")) {
            visto();
        }
    }
}