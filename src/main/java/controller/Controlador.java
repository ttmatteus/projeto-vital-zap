package controller;

// importa o usuário e a interface doida da janela principal
import model.Usuario;
import view.JanelaPrincipal;

public class Controlador {
    private Usuario usuario; // o desgraçado do usuário
    private JanelaPrincipal janela; // a janela toda bonitinha que exibe o zap genérico

    // construtor padrão, o básico do básico
    public Controlador() {
        this.usuario = new Usuario(); // cria o usuário do nada
        this.janela = new JanelaPrincipal(usuario); // passa o infeliz pro sistema
    }

    // construtor com injeção de dependência, caso queira fazer algo chique no futuro
    public Controlador(Usuario usuario, JanelaPrincipal janela) {
        this.usuario = usuario; // tá injetando na marra
        this.janela = janela;
    }

    // método que faz a bagaça começar
    public void iniciar() {
        // chama o método que exibe a interface e deixa o app rodando
        janela.exibir(); // e começa o show
    }

    // getter pra pegar o maldito usuário
    public Usuario getUsuario() {
        return usuario;
    }

    // getter pra acessar a bendita janela
    public JanelaPrincipal getJanela() {
        return janela;
    }
}
