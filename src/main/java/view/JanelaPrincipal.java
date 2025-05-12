package view;


import interfaces.Aplicativo;
import model.Mensagem;
import model.Usuario;
import observer.Observable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class JanelaPrincipal implements Aplicativo {
    // aqui são as variáveis principais da classe, tipo o esqueleto do app
    private final JFrame frame; // a janela principal do app
    private final Usuario usuario; // o usuário logado (você)
    private final Map<String, List<Mensagem>> mensagensPorChat; // armazena as mensagens de cada chat
    private final Observable observable; // controla notificações (tipo quando uma mensagem nova chega)
    private final List<UsuarioChat> usuarios; // lista de usuários do app
    private JPanel areaMensagens; // onde as mensagens do chat aparecem
    private JTextField campoMensagem; // o campo onde vc digita a mensagem
    private JButton botaoEnviar; // o botão de enviar mensagem
    private JPanel currentChatPanel; // o painel do chat que tá aberto agora
    private String currentChatName; // nome do chat atual (ex "obama pedro" meu heroi :) ass: tt)
    private JProgressBar storageBar; // barra de progresso do armazenamento
    private JProgressBar dataBar; // barra de progresso do uso de dados
    private JLabel storageInfo; // mostra quanto armazenamento tá sendo usado
    private JLabel dataInfo; // mostra quantos dados foram usados

    // mapas pra guardar imagens de perfil e números de telefone dos chats
    private final Map<String, String> chatImageMap = new HashMap<>();
    private final Map<String, String> chatNumberMap = new HashMap<>();
    private final Map<String, String> remetenteImageMap = new HashMap<>();

    // cores e fontes pra deixar o app bonitinho e moderno
    private static final Color COR_PRIMARIA = new Color(0, 122, 255); // azul vibrante pros botões e detalhes
    private static final Color COR_SECUNDARIA = new Color(255, 255, 255); // branco limpo pro fundo
    private static final Color COR_MENSAGEM_USUARIO = new Color(0, 122, 255); // azul pras mensagens que vc envia
    private static final Color COR_MENSAGEM_OUTRO = new Color(240, 242, 245); // cinza claro pras mensagens dos outros
    private static final Color COR_FUNDO = new Color(248, 250, 252); // fundo suave, quase branco
    private static final Color COR_SOMBRA = new Color(0, 0, 0, 30); // sombra sutil pra dar profundidade
    private static final Color COR_HOVER_CHAT = new Color(232, 236, 239); // cor quando vc passa o mouse por cima de um chat
    private static final Font FONT_MENSAGEM = new Font("Segoe UI", Font.PLAIN, 15); // fonte das mensagens
    private static final Font FONT_CAMPO = new Font("Segoe UI", Font.PLAIN, 16); // fonte do campo de texto
    private static final Font FONT_TITULO = new Font("Segoe UI", Font.BOLD, 18); // fonte dos títulos
    private static final Font FONT_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 14); // fonte dos subtítulos

    // classe interna pra criar avatares redondos (tipo a fotinha de perfil)
    private static class RoundedAvatarLabel extends JLabel {
        private Image image; // imagem do avatar
        private final int size; // tamanho do avatar

        public RoundedAvatarLabel(Image image, int size) {
            this.image = image;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false); // deixa o fundo transparente
        }

        @Override
        protected void paintComponent(Graphics g) {
            // essa porra aqui desenha a imagem redondinha, ou mete um emoji bizarro se n tiver imagem nenhuma
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, size, size);
            g2d.setClip(circle);

            if (image != null) {
                g2d.drawImage(image, 0, 0, size, size, this);
            } else {
                g2d.setColor(new Color(220, 220, 220));
                g2d.fill(circle);
                g2d.setColor(new Color(100, 100, 100));
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, size / 4));
                g2d.drawString("👤", size / 3, size / 2 + size / 12);
            }

            g2d.setClip(null);
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(circle);

            g2d.dispose();
        }
    }

    // classe interna pra criar botões redondos, tipo o de configurações
    private static class RoundButton extends JButton {
        private Color backgroundColor;

        public RoundButton() {
            super();
            this.backgroundColor = COR_SECUNDARIA;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(null);
            setPreferredSize(new Dimension(40, 40));
        }

        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            // desenha o botão como um círculo com a cor de fundo
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight());
            Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, size - 1, size - 1);

            g2d.setColor(backgroundColor);
            g2d.fill(circle);

            g2d.dispose();
            super.paintComponent(g);
        }

        @Override
        public boolean contains(int x, int y) {
            // verifica se o clique tá dentro do círculo
            int size = Math.min(getWidth(), getHeight());
            Ellipse2D circle = new Ellipse2D.Double(0, 0, size, size);
            return circle.contains(x, y);
        }
    }

    // classe interna pra criar badges arredondados, tipo os que mostram quantas mensagens novas tem
    private static class RoundedBadgeLabel extends JLabel {
        private static final int HEIGHT = 24;
        private static final int CORNER_RADIUS = 12;
        private static final int PADDING_X = 10;
        private static final int MIN_WIDTH = 24;

        public RoundedBadgeLabel(String text) {
            super(text);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Inter", Font.BOLD, 12));
            setForeground(Color.WHITE);

            // ajusta o tamanho do badge baseado no texto
            FontMetrics metrics = getFontMetrics(getFont());
            int textWidth = metrics.stringWidth(text);
            int width = Math.max(MIN_WIDTH, textWidth + PADDING_X * 2);
            setPreferredSize(new Dimension(width, HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            // desenha o badge como um retângulo arredondado
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            RoundRectangle2D rect = new RoundRectangle2D.Double(
                    0, 0, width - 1, height - 1, CORNER_RADIUS, CORNER_RADIUS);

            g2d.setColor(COR_PRIMARIA);
            g2d.fill(rect);

            g2d.setColor(COR_PRIMARIA);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(rect);

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    // construtor da janela principal, onde tudo começa, e o meu inferno começou :)
    public JanelaPrincipal(Usuario usuario) {
        this.usuario = usuario; // salva o usuário logado
        this.mensagensPorChat = new HashMap<>(); // inicia o mapa de mensagens
        this.observable = new Observable(); // inicia o sistema de notificações
        this.usuarios = new ArrayList<>(); // inicia a lista de usuários
        this.currentChatPanel = new JPanel(); // painel do chat atual (começa vazio)
        this.currentChatName = "Padrão"; // nome padrão do chat inicial

        inicializarUsuarios(); // configura os usuários iniciais

        // associa imagens de perfil pros chats
        chatImageMap.put("Obama Pedro", "/profile/obama.jpg");
        chatImageMap.put("Regina", "/profile/regina.jpg");
        chatImageMap.put("Marina", "/profile/marina.jpg");
        chatImageMap.put("Amigos Faculdade", "/profile/faculdade.jpg");
        chatImageMap.put("Trabalho", "/profile/trabalho.jpg");

        // associa números de telefone pros chats
        chatNumberMap.put("Obama Pedro", "11987654321");
        chatNumberMap.put("Regina", "11912345678");
        chatNumberMap.put("Marina", "11900000000");
        chatNumberMap.put("Amigos Faculdade", "11987651234");
        chatNumberMap.put("Trabalho", "11943218765");

        // associa imagens de perfil pros remetentes em grupos
        remetenteImageMap.put("Alicia", "/profile/alicia.jpg");
        remetenteImageMap.put("Marcos", "/profile/marcos.jpg");
        remetenteImageMap.put("Charles", "/profile/charles.jpg");
        remetenteImageMap.put("Jonas", "/profile/jonas.jpg");

        // configura a janela principal do app
        frame = new JFrame("Zap"); // nome da janela: "zap"
        frame.setSize(600, 700); // tamanho padrão da janela
        frame.setMinimumSize(new Dimension(350, 600)); // tamanho mínimo
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // fecha o app ao clicar no x
        frame.setLocationRelativeTo(null); // centraliza a janela na tela

        // painel principal com layout de cartas (pra trocar entre telas)
        JPanel mainPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(COR_FUNDO);

        // cria as telas do app
        JPanel chatsPanel = criarTelaChats(); // tela com a lista de chats
        JPanel chatPanel = criarTelaMensagens("Padrão"); // tela de mensagens (começa com chat padrão)
        JPanel profilePanel = criarTelaPerfil(); // tela de perfil
        JPanel settingsPanel = criarTelaConfiguracoes(); // tela de configurações

        // adiciona as telas ao painel principal
        mainPanel.add(chatsPanel, "Chats");
        mainPanel.add(chatPanel, "Chat");
        mainPanel.add(profilePanel, "Perfil");
        mainPanel.add(settingsPanel, "Configurações");

        frame.setContentPane(mainPanel); // define o painel principal como conteúdo da janela

        // cria a barra de ferramentas (em cima, com botões de navegação)
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false); // não deixa a barra ser arrastada
        toolBar.setBackground(COR_SECUNDARIA);
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220))); // borda sutil
        JButton chatsButton = criarBotaoToolbar("Chats"); // botão pra ir pra lista de chats
        JButton profileButton = criarBotaoToolbar("Perfil"); // botão pra ir pro perfil
        toolBar.add(chatsButton);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(profileButton);

        // ações dos botões da toolbar.
        chatsButton.addActionListener(e -> ((CardLayout) mainPanel.getLayout()).show(mainPanel, "Chats"));
        profileButton.addActionListener(e -> {
            currentChatName = usuario.getNumero() != null ? "Você" : "Padrão";
            ((CardLayout) mainPanel.getLayout()).show(mainPanel, "Perfil");
        });

        frame.add(toolBar, BorderLayout.NORTH); // adiciona a toolbar no topo

        // listener pra redimensionar a área de mensagens quando a janela mudar de tamanho
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                atualizarAreaMensagens();
            }
        });
    }

    // cria os botões da toolbar (os de "chats" e "perfil")
    private JButton criarBotaoToolbar(String texto) {
        JButton botao = new JButton(texto);
        botao.setFont(FONT_SUBTITULO);
        botao.setForeground(new Color(60, 60, 60));
        botao.setBackground(COR_SECUNDARIA);
        botao.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                botao.setBackground(new Color(240, 240, 240)); // muda a cor ao passar o mouse
            }

            @Override
            public void mouseExited(MouseEvent e) {
                botao.setBackground(COR_SECUNDARIA); // volta a cor normal
            }
        });
        return botao;
    }

    // inicializa os usuários do app (você e outros fictícios)
    private void inicializarUsuarios() {
        UsuarioChat usuario1 = new UsuarioChat("Você"); // você, o usuário logado
        UsuarioChat usuario2 = new UsuarioChat("João Silva"); // outro usuário fictício
        UsuarioChat usuario3 = new UsuarioChat("Capy"); // mais um usuário fictício
        usuarios.add(usuario1);
        usuarios.add(usuario2);
        usuarios.add(usuario3);

        // adiciona os usuários como observadores pra receber notificações
        for (UsuarioChat u : usuarios) {
            observable.adicionarObserver(u);
        }

        permissao(); // verifica permissões iniciais
    }

    // mostra a tela de login (onde vc digita seu número de celular)
    public void exibir() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                // desenha o fundo com um gradiente suave
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(245, 250, 255),
                        0, getHeight(), COR_SECUNDARIA
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Digite seu número de celular:");
        titleLabel.setFont(FONT_TITULO);
        titleLabel.setForeground(new Color(30, 30, 30));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextField numeroField = new JTextField(15); // campo pra digitar o número
        numeroField.setFont(FONT_CAMPO);
        numeroField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        numeroField.setBackground(COR_FUNDO);
        panel.add(numeroField, BorderLayout.CENTER);

        // configura o estilo dos botões do diálogo
        UIManager.put("Button.background", COR_PRIMARIA);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", FONT_SUBTITULO);
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // mostra o diálogo de login
        JOptionPane optionPane = new JOptionPane(
                panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );
        JDialog dialog = optionPane.createDialog(frame, "Login");
        dialog.getContentPane().setBackground(COR_SECUNDARIA);
        dialog.setVisible(true);

        Integer result = (Integer) optionPane.getValue();
        String numero = (result != null && result == JOptionPane.OK_OPTION) ? numeroField.getText().trim() : null;

        // valida o número (tem que ter 10 a 13 dígitos)
        while (numero == null || !numero.matches("\\d{10,13}")) {
            titleLabel.setText("Número inválido. Tente novamente:");
            titleLabel.setForeground(new Color(255, 80, 80));
            optionPane.setValue(null);
            dialog.setVisible(true);
            result = (Integer) optionPane.getValue();
            numero = (result != null && result == JOptionPane.OK_OPTION) ? numeroField.getText().trim() : null;
        }

        usuario.setNumero(numero); // salva o número do usuário

        // mostra uma mensagem de boas-vindas
        JPanel welcomePanel = new JPanel(new BorderLayout(10, 10));
        welcomePanel.setBackground(COR_SECUNDARIA);
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel welcomeLabel = new JLabel("Bem-vindo, número " + numero + "!");
        welcomeLabel.setFont(FONT_TITULO);
        welcomeLabel.setForeground(new Color(30, 30, 30));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        JOptionPane welcomePane = new JOptionPane(
                welcomePanel,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION
        );
        JDialog welcomeDialog = welcomePane.createDialog(frame, "Sucesso");
        welcomeDialog.getContentPane().setBackground(COR_SECUNDARIA);
        welcomeDialog.setVisible(true);

        frame.setVisible(true); // mostra a janela principal
    }

    // redimensiona imagens pra caber nos avatares e ícones
    private Image getScaledImage(Image original, int width, int height) {
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaledImage;
    }

    // pega a última mensagem de um chat pra mostrar na lista de chats
    private String[] getUltimaMensagem(String chatName) {
        List<Mensagem> mensagensFixas = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // mensagens fictícias pra cada chat (só pra simular o app)
        if (chatName.equals("Obama Pedro")) {
            mensagensFixas.add(new Mensagem("Eae seu vacilão, bora fechar aquela trip pra Chapada?", chatName, LocalTime.of(10, 15), false, chatName));
            mensagensFixas.add(new Mensagem("Tô suave, só quero trilha, cachoeira e céu limpo. Bora meter o louco", "Você", LocalTime.of(10, 16), true, chatName));
            mensagensFixas.add(new Mensagem("Já viu os rolê? Tem uma trilha noturna que é doida", chatName, LocalTime.of(10, 17), false, chatName));
            mensagensFixas.add(new Mensagem("Ainda não, manda aí. E vê quem vai levar o rango, cê é mó mão de vaca", "Você", LocalTime.of(10, 18), true, chatName));
        } else if (chatName.equals("Regina")) {
            mensagensFixas.add(new Mensagem("Oi, tu viu aquele trailer novo do remake do Cyberpunk? Achei bem legal", chatName, LocalTime.of(14, 30), false, chatName));
            mensagensFixas.add(new Mensagem("Vi sim. Já botei na lista. Tu joga onde?", "Você", LocalTime.of(14, 31), true, chatName));
            mensagensFixas.add(new Mensagem("No PC. Meu setup tá rodando lisinho agora. Se quiser jogar no fds...", chatName, LocalTime.of(14, 32), false, chatName));
            mensagensFixas.add(new Mensagem("Fechou. Mas se eu te amassar, nem vem chorar depois", "Você", LocalTime.of(14, 33), true, chatName));
        } else if (chatName.equals("Marina")) {
            mensagensFixas.add(new Mensagem("Eai porra, cê comprou o ingresso do fest? Essa line tá um escândalo", chatName, LocalTime.of(18, 45), false, chatName));
            mensagensFixas.add(new Mensagem("Comprei sim, tá maluca? Foo Fighters e Arctic? Vou quebrar tudo", "Você", LocalTime.of(18, 46), true, chatName));
            mensagensFixas.add(new Mensagem("Vou meter um look todo ferrado e dark. Tu vai como?", chatName, LocalTime.of(18, 47), false, chatName));
            mensagensFixas.add(new Mensagem("Tô pensando numa jaqueta braba e coturno. A gente vai chamar atenção kkk", "Você", LocalTime.of(18, 48), true, chatName));
        } else if (chatName.equals("Amigos Faculdade")) {
            mensagensFixas.add(new Mensagem("Eae, q filme vai ser sábado?", "Alicia", LocalTime.of(9, 10), false, chatName));
            mensagensFixas.add(new Mensagem("Terrorzão estilo Hereditário, vamo?", "Marcos", LocalTime.of(9, 12), false, chatName));
            mensagensFixas.add(new Mensagem("Cê só pensa em possessão, velho. Bora ver um sci-fi, Duna por ex", "Você", LocalTime.of(9, 15), true, chatName));
            mensagensFixas.add(new Mensagem("Duna é top, mas só se for no IMAX", "Charles", LocalTime.of(9, 17), false, chatName));
        } else if (chatName.equals("Trabalho")) {
            mensagensFixas.add(new Mensagem("Pessoal, alguém conseguiu resolver o bug do endpoint de login?", "Marcos", LocalTime.of(15, 20), false, chatName));
            mensagensFixas.add(new Mensagem("Tava com conflito no middleware. Já subi uma correção na dev", "Você", LocalTime.of(15, 22), true, chatName));
            mensagensFixas.add(new Mensagem("Boa! A branch tá com o nome `fix/login-handler`?", "Charles", LocalTime.of(15, 25), false, chatName));
            mensagensFixas.add(new Mensagem("Isso. Só revisar antes de dar merge, tem uma alteração no token também", "Você", LocalTime.of(15, 27), true, chatName));
            mensagensFixas.add(new Mensagem("Jonas, tu consegue rodar os testes de integração hoje ainda?", "Marcos", LocalTime.of(15, 28), false, chatName));
            mensagensFixas.add(new Mensagem("Consigo sim, tô finalizando uma call aqui e já rodo", "Jonas", LocalTime.of(15, 30), false, chatName));
            mensagensFixas.add(new Mensagem("Fechou. Se passar tudo, a gente já manda pra homologação", "Você", LocalTime.of(15, 32), true, chatName));
        } else {
            mensagensFixas.add(new Mensagem("Eae, tranquilo?", chatName, LocalTime.of(22, 20), false, chatName));
            mensagensFixas.add(new Mensagem("Tô precisando de um logo novo pro grupo", chatName, LocalTime.of(22, 21), false, chatName));
            mensagensFixas.add(new Mensagem("Algo mais direto, nada exagerado", chatName, LocalTime.of(22, 22), false, chatName));
            mensagensFixas.add(new Mensagem("Tranquilo. Semana que vem começo. Manda os docs hj", "Você", LocalTime.of(22, 23), true, chatName));
            mensagensFixas.add(new Mensagem("Fechou, valeu", chatName, LocalTime.of(22, 24), false, chatName));
        }

        // combina mensagens fictícias com as mensagens reais do chat
        List<Mensagem> mensagensDoChat = mensagensPorChat.getOrDefault(chatName, new ArrayList<>());
        List<Mensagem> todasMensagens = new ArrayList<>(mensagensFixas);
        todasMensagens.addAll(mensagensDoChat);

        // encontra a última mensagem do chat
        Mensagem ultimaMensagem = null;
        LocalTime ultimoHorario = LocalTime.MIN;
        for (Mensagem msg : todasMensagens) {
            if (msg.getHorario().isAfter(ultimoHorario)) {
                ultimaMensagem = msg;
                ultimoHorario = msg.getHorario();
            }
        }

        if (ultimaMensagem != null) {
            String mensagemTexto = ultimaMensagem.getTexto();
            String remetente = ultimaMensagem.getRemetente();
            String horario = ultimaMensagem.getHorario().format(formatter);
            if (chatName.equals("Grupo de Cinema") || chatName.equals("Amigos Faculdade")) {
                mensagemTexto = remetente + ": " + mensagemTexto; // mostra o remetente em grupos
            }
            if (mensagemTexto.length() > 50) {
                mensagemTexto = mensagemTexto.substring(0, 47) + "..."; // corta mensagens longas
            }
            return new String[]{mensagemTexto, horario};
        }

        return new String[]{"Sem mensagens", "00:00"};
    }

    // atualiza a tela de chats (pra refletir novas mensagens, por exemplo)
    private void atualizarTelaChats() {
        JPanel mainPanel = (JPanel) frame.getContentPane();
        removeIf(mainPanel, p -> p.getName() != null && p.getName().equals("Chats"));
        JPanel chatsPanel = criarTelaChats();
        mainPanel.add(chatsPanel, "Chats");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // remove componentes do painel baseado numa condição
    private void removeIf(Container container, Predicate<Component> condition) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (condition.test(component)) {
                container.remove(component);
            }
        }
    }

    // cria a tela com a lista de chats
    private JPanel criarTelaChats() {
        JPanel panelChats = new JPanel();
        panelChats.setLayout(new BoxLayout(panelChats, BoxLayout.Y_AXIS)); // organiza os chats em uma coluna
        panelChats.setBackground(COR_SECUNDARIA);
        panelChats.setBorder(new EmptyBorder(15, 15, 15, 15));

        // painel do cabeçalho (onde fica o botão de configurações)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // botão de configurações (no canto superior direito)
        RoundButton settingsButton = new RoundButton();
        ImageIcon settingsImageIcon = null;
        try {
            URL resourceUrl = getClass().getResource("/icons/settings.png");
            if (resourceUrl == null) {
                throw new IllegalArgumentException("Resource not found: /icons/settings.png");
            }
            ImageIcon icon = new ImageIcon(resourceUrl);
            if (icon.getImage() != null) {
                Image scaledImage = getScaledImage(icon.getImage(), 24, 24);
                settingsImageIcon = new ImageIcon(scaledImage);
            }
        } catch (Exception ex) {
            System.err.println("Erro ao carregar ícone de configurações: /icons/settings.png");
            ex.printStackTrace();
            settingsButton.setText("⚙");
            settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        }
        settingsButton.setIcon(settingsImageIcon);
        settingsButton.setBackgroundColor(COR_SECUNDARIA);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                atualizarConfiguracoes();
                ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Configurações");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                settingsButton.setBackgroundColor(new Color(240, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                settingsButton.setBackgroundColor(COR_SECUNDARIA);
            }
        });

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        settingsPanel.setOpaque(false);
        settingsPanel.add(settingsButton);
        headerPanel.add(settingsPanel, BorderLayout.EAST);

        // lista de chats (fixa, só pra simular)
        String[][] chats = {
                {"Obama Pedro", "/profile/obama.jpg", "", "", "2"},
                {"Regina", "/profile/regina.jpg", "", "", "0"},
                {"Marina", "/profile/marina.jpg", "", "", "13"},
                {"Amigos Faculdade", "/profile/faculdade.jpg", "", "", "0"},
                {"Trabalho", "/profile/trabalho.jpg", "", "", "3"}
        };

        // cria um item pra cada chat na lista
        for (String[] chat : chats) {
            String chatName = chat[0];
            String[] ultimaMensagemInfo = getUltimaMensagem(chatName);
            chat[2] = ultimaMensagemInfo[0];
            chat[3] = ultimaMensagemInfo[1];

            // painel de cada chat na lista
            JPanel chatItem = new JPanel(new BorderLayout(10, 10)) {
                private Color bgColor = COR_SECUNDARIA;

                @Override
                protected void paintComponent(Graphics g) {
                    // desenha o chat com bordas arredondadas e sombra
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(bgColor);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2d.setColor(COR_SOMBRA);
                    g2d.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 20, 20);
                    g2d.setColor(bgColor);
                    g2d.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                    g2d.dispose();
                }

                public void setBackgroundColor(Color color) {
                    this.bgColor = color;
                    repaint();
                }
            };
            chatItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            chatItem.setBorder(new EmptyBorder(10, 10, 10, 10));
            chatItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chatItem.setOpaque(false);

            // carrega a imagem de perfil do chat
            Image profileImage = null;
            try {
                URL resourceUrl = getClass().getResource(chat[1]);
                if (resourceUrl == null) {
                    throw new IllegalArgumentException("Resource not found: " + chat[1]);
                }
                ImageIcon profileIcon = new ImageIcon(resourceUrl);
                if (profileIcon.getImage() != null) {
                    profileImage = getScaledImage(profileIcon.getImage(), 60, 60);
                }
            } catch (Exception ex) {
                System.err.println("Erro ao carregar imagem de perfil: " + chat[1]);
                ex.printStackTrace();
            }
            RoundedAvatarLabel profileLabel = new RoundedAvatarLabel(profileImage, 60);

            // painel com o nome e a última mensagem do chat
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

            JLabel nameLabel = new JLabel(chat[0]);
            nameLabel.setFont(FONT_TITULO);
            nameLabel.setForeground(new Color(30, 30, 30));

            JLabel messageLabel = new JLabel("<html>" + chat[2].replace("\n", "<br>") + "</html>");
            messageLabel.setFont(FONT_SUBTITULO);
            messageLabel.setForeground(new Color(100, 100, 100));

            textPanel.add(nameLabel);
            textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            textPanel.add(messageLabel);

            // painel com o horário e o badge de mensagens novas
            JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
            rightPanel.setOpaque(false);
            JLabel timeLabel = new JLabel(chat[3]);
            timeLabel.setFont(FONT_SUBTITULO);
            timeLabel.setForeground(new Color(120, 120, 120));
            if (!chat[4].equals("0")) {
                RoundedBadgeLabel badgeLabel = new RoundedBadgeLabel(chat[4]);
                badgeLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                rightPanel.add(badgeLabel, BorderLayout.SOUTH);
            }
            rightPanel.add(timeLabel, BorderLayout.NORTH);

            chatItem.add(profileLabel, BorderLayout.WEST);
            chatItem.add(textPanel, BorderLayout.CENTER);
            chatItem.add(rightPanel, BorderLayout.EAST);

            // ação ao clicar no chat
            final String finalChatName = chat[0];
            chatItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    currentChatName = finalChatName;
                    currentChatPanel = criarTelaMensagens(finalChatName);
                    frame.getContentPane().add(currentChatPanel, "Chat");
                    ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Chat");
                    observable.notificarObservers("Nova mensagem no chat " + finalChatName);
                    atualizarAreaMensagens();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    JPanel panel = (JPanel) e.getSource();
                    try {
                        java.lang.reflect.Method method = panel.getClass().getMethod("setBackgroundColor", Color.class);
                        method.invoke(panel, COR_HOVER_CHAT);
                    } catch (Exception ex) {
                        panel.setBackground(COR_HOVER_CHAT);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    JPanel panel = (JPanel) e.getSource();
                    try {
                        java.lang.reflect.Method method = panel.getClass().getMethod("setBackgroundColor", Color.class);
                        method.invoke(panel, COR_SECUNDARIA);
                    } catch (Exception ex) {
                        panel.setBackground(COR_SECUNDARIA);
                    }
                }
            });

            panelChats.add(chatItem);
            panelChats.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // painel principal com o cabeçalho e a lista de chats
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(headerPanel, BorderLayout.NORTH);
        container.add(new JScrollPane(panelChats) {{
            setBorder(BorderFactory.createEmptyBorder());
            getVerticalScrollBar().setUnitIncrement(16);
            setOpaque(false);
            getViewport().setOpaque(false);
        }}, BorderLayout.CENTER);

        return container;
    }

    // cria a tela de mensagens de um chat específico
    private JPanel criarTelaMensagens(String chatName) {
        JPanel panelMensagens = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                // desenha o fundo com um gradiente suave
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(245, 250, 255),
                        0, getHeight(), COR_FUNDO
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelMensagens.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelMensagens.setName(chatName);

        // cabeçalho da tela de mensagens (com botão de voltar, avatar e nome)
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel backButton = new JLabel("←"); // botão pra voltar pra lista de chats
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Chats");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                backButton.setForeground(COR_PRIMARIA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backButton.setForeground(new Color(60, 60, 60));
            }
        });

        // carrega o avatar do chat
        Image profileImage = null;
        String normalizedChatName = chatName.trim();
        String imagePath = chatImageMap.get(normalizedChatName);

        if (imagePath != null) {
            try {
                URL resourceUrl = getClass().getResource(imagePath);
                if (resourceUrl == null) {
                    throw new IllegalArgumentException("Resource not found: " + imagePath);
                }
                ImageIcon profileIcon = new ImageIcon(resourceUrl);
                if (profileIcon.getImage() != null) {
                    profileImage = getScaledImage(profileIcon.getImage(), 50, 50);
                }
            } catch (Exception ex) {
                System.err.println("Erro ao carregar imagem para " + normalizedChatName + ": " + imagePath);
                ex.printStackTrace();
            }
        }

        RoundedAvatarLabel profileLabel = new RoundedAvatarLabel(profileImage, 50);
        profileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentChatName = normalizedChatName;
                JPanel newProfilePanel = criarTelaPerfil();
                frame.getContentPane().add(newProfilePanel, "Perfil");
                ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Perfil");
            }
        });

        JLabel nameLabel = new JLabel(chatName); // nome do chat no cabeçalho
        nameLabel.setFont(FONT_TITULO);
        nameLabel.setForeground(new Color(30, 30, 30));

        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(profileLabel, BorderLayout.CENTER);
        headerPanel.add(nameLabel, BorderLayout.EAST);

        // área onde as mensagens aparecem
        areaMensagens = new JPanel();
        areaMensagens.setLayout(new BoxLayout(areaMensagens, BoxLayout.Y_AXIS));
        areaMensagens.setOpaque(false);
        areaMensagens.setBorder(new EmptyBorder(10, 10, 10, 10));

        // scroll pra rolar as mensagens
        JScrollPane scrollPane = new JScrollPane(areaMensagens);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

// tornar a barra de rolagem "invisível", mas funcional
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                // não desenha o fundo da barra de rolagem (track)
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isDagging = ((JScrollBar) c).getModel().getValueIsAdjusting();
                g2.setColor(isDagging ? new Color(80,80,80,120) : new Color(120,120,120,80));

                int width = 6;
                int height = thumbBounds.height;
                int x = thumbBounds.x + (thumbBounds.width - width) / 2;
                int y = thumbBounds.y;
                int arc = 6;

                g2.fillRoundRect(x, y, width, height, arc, arc);
                g2.dispose();
            }

            @Override
            protected Dimension getMinimumThumbSize() {
                return new Dimension(6, 20);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                // remove o botão de seta para cima
                return createInvisibleButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                // remove o botão de seta para baixo
                return createInvisibleButton();
            }

            // botão invisível que some, mas continua ali funcionando, poq a gente quer estilo minimalista né? (estou morrendo)
            private JButton createInvisibleButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0)); // tamanho zero para não ocupar espaço
                button.setVisible(false); // torna o botão invisível
                button.setOpaque(false); // garante que o botão não tenha fundo
                return button;
            }

        });


        // campo onde você digita a mensagem
        campoMensagem = new JTextField();
        campoMensagem.setFont(FONT_CAMPO);
        campoMensagem.setBackground(COR_SECUNDARIA);
        campoMensagem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(12, 15, 12, 15)
        ));

        // envia a mensagem ao apertar enter
        campoMensagem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enviarMensagem();
                }
            }
        });

        // botão de enviar mensagem
        botaoEnviar = new JButton();
        ImageIcon sendImageIcon = null;
        try {
            URL resourceUrl = getClass().getResource("/icons/send.png");
            if (resourceUrl == null) {
                throw new IllegalArgumentException("Resource not found: /icons/send.png");
            }
            ImageIcon icon = new ImageIcon(resourceUrl);
            if (icon.getImage() != null) {
                Image scaledImage = getScaledImage(icon.getImage(), 24, 24);
                sendImageIcon = new ImageIcon(scaledImage);
            }
        } catch (Exception ex) {
            // N/T puta merda, o ícone de envio deu pau de novo - Que vida boa :P
            System.err.println("Erro ao carregar ícone de envio: /icons/send.png");
            ex.printStackTrace();
            botaoEnviar.setText("→");
            botaoEnviar.setFont(new Font("Arial Unicode MS", Font.BOLD, 18));
        }
        botaoEnviar.setIcon(sendImageIcon);
        botaoEnviar.setBackground(COR_PRIMARIA);
        botaoEnviar.setForeground(Color.WHITE);
        botaoEnviar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        botaoEnviar.setFocusPainted(false);
        botaoEnviar.setPreferredSize(new Dimension(50, 50));
        botaoEnviar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botaoEnviar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                botaoEnviar.setBackground(new Color(0, 100, 220));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                botaoEnviar.setBackground(COR_PRIMARIA);
            }
        });

        botaoEnviar.addActionListener(e -> enviarMensagem());

        // painel inferior com o campo de mensagem e o botão de enviar
        JPanel painelInferior = new JPanel();
        painelInferior.setLayout(new BoxLayout(painelInferior, BoxLayout.X_AXIS));
        painelInferior.setOpaque(false);
        painelInferior.setBorder(new EmptyBorder(10, 10, 10, 10));

        painelInferior.add(campoMensagem);
        painelInferior.add(Box.createRigidArea(new Dimension(10, 0)));
        painelInferior.add(botaoEnviar);

        panelMensagens.add(headerPanel, BorderLayout.NORTH);
        panelMensagens.add(scrollPane, BorderLayout.CENTER);
        panelMensagens.add(painelInferior, BorderLayout.SOUTH);

        atualizarAreaMensagens(); // atualiza as mensagens exibidas

        return panelMensagens;
    }

    // cria a tela de perfil (mostra a foto, nome e número do usuário ou chat)
    private JPanel criarTelaPerfil() {
        JPanel panelPerfil = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // desenha o fundo com gradiente.
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(245, 250, 255),
                        0, getHeight(), COR_SECUNDARIA
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelPerfil.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // botão de voltar.
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backPanel.setOpaque(false);
        JLabel backButton = new JLabel("←");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Chat");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                backButton.setForeground(COR_PRIMARIA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backButton.setForeground(new Color(60, 60, 60));
            }
        });
        backPanel.add(backButton);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panelPerfil.add(backPanel, gbc);

        // painel com a foto de perfil
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setOpaque(false);
        imagePanel.setMaximumSize(new Dimension(160, 160));

        String nomeParaBusca = currentChatName != null ? currentChatName.trim() : "Padrão";
        String imagePath = chatImageMap.get(nomeParaBusca);
        Image profileImage = null;

        if (imagePath != null) {
            try {
                URL resourceUrl = getClass().getResource(imagePath);
                if (resourceUrl == null) {
                    throw new IllegalArgumentException("Resource not found: " + imagePath);
                }
                ImageIcon profileIcon = new ImageIcon(resourceUrl);
                if (profileIcon.getImage() != null) {
                    profileImage = getScaledImage(profileIcon.getImage(), 160, 160);
                }
            } catch (Exception ex) {
                System.err.println("Erro ao carregar imagem de perfil para " + nomeParaBusca + ": " + imagePath);
                ex.printStackTrace();
            }
        }

        RoundedAvatarLabel profileLabel = new RoundedAvatarLabel(profileImage, 160);
        GridBagConstraints gbcImage = new GridBagConstraints();
        gbcImage.gridx = 0;
        gbcImage.gridy = 0;
        imagePanel.add(profileLabel, gbcImage);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panelPerfil.add(imagePanel, gbc);

        // painel com as informações (nome, número e status)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel nameLabel = new JLabel(nomeParaBusca);
        nameLabel.setFont(FONT_TITULO);
        nameLabel.setForeground(new Color(30, 30, 30));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String numero = chatNumberMap.getOrDefault(nomeParaBusca, "Desconhecido");
        if (nomeParaBusca.equals("Você") && usuario.getNumero() != null) {
            numero = usuario.getNumero();
        }
        JLabel numberLabel = new JLabel(numero);
        numberLabel.setFont(FONT_SUBTITULO);
        numberLabel.setForeground(new Color(100, 100, 100));
        numberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel("Online");
        statusLabel.setFont(FONT_SUBTITULO);
        statusLabel.setForeground(new Color(0, 200, 0));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(numberLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(statusLabel);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelPerfil.add(infoPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        panelPerfil.add(new JLabel(), gbc);

        return panelPerfil;
    }

    // cria a tela de configurações (mostra armazenamento e uso de dados)
    private JPanel criarTelaConfiguracoes() {
        JPanel panelSettings = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // desenha o fundo com gradiente
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(245, 250, 255),
                        0, getHeight(), COR_SECUNDARIA
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelSettings.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // botão de voltar
        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backPanel.setOpaque(false);
        JLabel backButton = new JLabel("←");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ((CardLayout) frame.getContentPane().getLayout()).show(frame.getContentPane(), "Chats");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                backButton.setForeground(COR_PRIMARIA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backButton.setForeground(new Color(60, 60, 60));
            }
        });
        backPanel.add(backButton);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panelSettings.add(backPanel, gbc);

        // título da tela
        JLabel titleLabel = new JLabel("Configurações");
        titleLabel.setFont(FONT_TITULO);
        titleLabel.setForeground(new Color(30, 30, 30));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelSettings.add(titleLabel, gbc);

        // painel com as barras de armazenamento e dados
        JPanel usagePanel = new JPanel();
        usagePanel.setLayout(new BoxLayout(usagePanel, BoxLayout.Y_AXIS));
        usagePanel.setBackground(COR_SECUNDARIA);
        usagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        usagePanel.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));

        JLabel storageLabel = new JLabel("Uso de Armazenamento");
        storageLabel.setFont(FONT_SUBTITULO);
        storageLabel.setForeground(new Color(30, 30, 30));
        storageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        storageBar = new JProgressBar(0, 5000); // barra de armazenamento (máximo 5 gb)
        storageBar.setValue(0);
        storageBar.setForeground(COR_PRIMARIA);
        storageBar.setBackground(new Color(220, 220, 220));
        storageBar.setBorderPainted(false);
        storageBar.setPreferredSize(new Dimension(300, 12));

        storageInfo = new JLabel("0 MB usados de 5 GB");
        storageInfo.setFont(FONT_SUBTITULO);
        storageInfo.setForeground(new Color(100, 100, 100));
        storageInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        usagePanel.add(storageLabel);
        usagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        usagePanel.add(storageBar);
        usagePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        usagePanel.add(storageInfo);
        usagePanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel dataLabel = new JLabel("Uso de Dados");
        dataLabel.setFont(FONT_SUBTITULO);
        dataLabel.setForeground(new Color(30, 30, 30));
        dataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dataBar = new JProgressBar(0, 2000); // barra de dados (máximo 2 gb)
        dataBar.setValue(0);
        dataBar.setForeground(COR_PRIMARIA);
        dataBar.setBackground(new Color(220, 220, 220));
        dataBar.setBorderPainted(false);
        dataBar.setPreferredSize(new Dimension(300, 12));

        dataInfo = new JLabel("0 GB usados este mês");
        dataInfo.setFont(FONT_SUBTITULO);
        dataInfo.setForeground(new Color(100, 100, 100));
        dataInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        usagePanel.add(dataLabel);
        usagePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        usagePanel.add(dataBar);
        usagePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        usagePanel.add(dataInfo);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelSettings.add(usagePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        panelSettings.add(new JLabel(), gbc);

        atualizarConfiguracoes(); // atualiza as informações de armazenamento e dados

        return panelSettings;
    }

    // atualiza as barras de armazenamento e uso de dados
    private void atualizarConfiguracoes() {
        UsuarioChat usuarioAtual = usuarios.stream()
                .filter(u -> u.getNome().equals("Você"))
                .findFirst()
                .orElse(null);
        if (usuarioAtual != null) {
            int armazenamento = usuarioAtual.getArmazenamentoUsado();
            int dados = usuarioAtual.getDadosUsados();
            storageBar.setValue(armazenamento);
            storageInfo.setText(armazenamento + " MB usados de 5 GB");
            dataBar.setValue(dados);
            dataInfo.setText(dados + " MB usados este mês");
        }
    }

    // envia uma mensagem nova
    private void enviarMensagem() {
        String texto = campoMensagem.getText().trim();
        if (texto.isEmpty()) {
            // mostra um aviso se tentar enviar mensagem vazia
            JPanel errorPanel = new JPanel(new BorderLayout(10, 10));
            errorPanel.setBackground(COR_SECUNDARIA);
            errorPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel errorLabel = new JLabel("Mensagens vazias não podem ser enviadas.");
            errorLabel.setFont(FONT_TITULO);
            errorLabel.setForeground(new Color(255, 80, 80));
            errorPanel.add(errorLabel, BorderLayout.CENTER);

            JOptionPane errorPane = new JOptionPane(
                    errorPanel,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.DEFAULT_OPTION
            );
            JDialog errorDialog = errorPane.createDialog(frame, "Aviso");
            errorDialog.getContentPane().setBackground(COR_SECUNDARIA);
            errorDialog.setVisible(true);
            return;
        }

        // ah, agora vem a burocracia precisa de permissão pra enviar msg, serio mesmo?
        UsuarioChat usuarioAtual = usuarios.stream()
                .filter(u -> u.getNome().equals("Você"))
                .findFirst()
                .orElse(null);
        if (usuarioAtual == null || !usuarioAtual.temPermissao()) {
            int resposta = permissaoDialog();
            if (resposta == JOptionPane.YES_OPTION) {
                usuarioAtual.permite();
            } else {
                return;
            }
        }

        // adiciona a mensagem ao chat
        mensagensPorChat.computeIfAbsent(currentChatName, k -> new ArrayList<>());
        Mensagem msg = new Mensagem(texto, "Você", currentChatName);
        mensagensPorChat.get(currentChatName).add(msg);

        campoMensagem.setText(""); // limpa o campo de mensagem
        notifica(); // notifica os observadores
        armazenamento(); // atualiza o armazenamento
        usoDeDados(); // atualiza o uso de dados
        atualizarAreaMensagens(); // atualiza a tela de mensagens
        atualizarTelaChats(); // atualiza a lista de chats
        atualizarConfiguracoes(); // atualiza as configurações
    }

    // atualiza a área de mensagens do chat atual
    private void atualizarAreaMensagens() {
        areaMensagens.removeAll();

        String chatName = currentChatName != null ? currentChatName : "Padrão";
        List<Mensagem> mensagensFixas = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // carrega mensagens fictícias pro chat (só pra simular)
        if (chatName.equals("Obama Pedro")) {
            mensagensFixas.add(new Mensagem("Eae seu vacilão, bora fechar aquela trip pra Chapada?", chatName, LocalTime.of(10, 15), false, chatName));
            mensagensFixas.add(new Mensagem("Tô suave, só quero trilha, cachoeira e céu limpo. Bora meter o louco", "Você", LocalTime.of(10, 16), true, chatName));
            mensagensFixas.add(new Mensagem("Já viu os rolê? Tem uma trilha noturna que é doida", chatName, LocalTime.of(10, 17), false, chatName));
            mensagensFixas.add(new Mensagem("Ainda não, manda aí. E vê quem vai levar o rango, cê é mó mão de vaca", "Você", LocalTime.of(10, 18), true, chatName));
        } else if (chatName.equals("Regina")) {
            mensagensFixas.add(new Mensagem("Oi, tu viu aquele trailer novo do remake do Cyberpunk? Achei bem legal", chatName, LocalTime.of(14, 30), false, chatName));
            mensagensFixas.add(new Mensagem("Vi sim. Já botei na lista. Tu joga onde?", "Você", LocalTime.of(14, 31), true, chatName));
            mensagensFixas.add(new Mensagem("No PC. Meu setup tá rodando lisinho agora. Se quiser jogar no fds...", chatName, LocalTime.of(14, 32), false, chatName));
            mensagensFixas.add(new Mensagem("Fechou. Mas se eu te amassar, nem vem chorar depois", "Você", LocalTime.of(14, 33), true, chatName));
        } else if (chatName.equals("Marina")) {
            mensagensFixas.add(new Mensagem("Eai porra, cê comprou o ingresso do fest? Essa line tá um escândalo", chatName, LocalTime.of(18, 45), false, chatName));
            mensagensFixas.add(new Mensagem("Comprei sim, tá maluca? Foo Fighters e Arctic? Vou quebrar tudo", "Você", LocalTime.of(18, 46), true, chatName));
            mensagensFixas.add(new Mensagem("Vou meter um look todo ferrado e dark. Tu vai como?", chatName, LocalTime.of(18, 47), false, chatName));
            mensagensFixas.add(new Mensagem("Tô pensando numa jaqueta braba e coturno. A gente vai chamar atenção kkk", "Você", LocalTime.of(18, 48), true, chatName));
        } else if (chatName.equals("Amigos Faculdade")) {
            mensagensFixas.add(new Mensagem("Eae, q filme vai ser sábado?", "Alicia", LocalTime.of(9, 10), false, chatName));
            mensagensFixas.add(new Mensagem("Terrorzão estilo Hereditário, vamo?", "Marcos", LocalTime.of(9, 12), false, chatName));
            mensagensFixas.add(new Mensagem("Cê só pensa em possessão, velho. Bora ver um sci-fi, Duna por ex", "Você", LocalTime.of(9, 15), true, chatName));
            mensagensFixas.add(new Mensagem("Duna é top, mas só se for no IMAX", "Charles", LocalTime.of(9, 17), false, chatName));
        } else if (chatName.equals("Trabalho")) {
            mensagensFixas.add(new Mensagem("Pessoal, alguém conseguiu resolver o bug do endpoint de login?", "Marcos", LocalTime.of(15, 20), false, chatName));
            mensagensFixas.add(new Mensagem("Tava com conflito no middleware. Já subi uma correção na dev", "Você", LocalTime.of(15, 22), true, chatName));
            mensagensFixas.add(new Mensagem("Boa! A branch tá com o nome `fix/login-handler`?", "Charles", LocalTime.of(15, 25), false, chatName));
            mensagensFixas.add(new Mensagem("Isso. Só revisar antes de dar merge, tem uma alteração no token também", "Você", LocalTime.of(15, 27), true, chatName));
            mensagensFixas.add(new Mensagem("Jonas, tu consegue rodar os testes de integração hoje ainda?", "Marcos", LocalTime.of(15, 28), false, chatName));
            mensagensFixas.add(new Mensagem("Consigo sim, tô finalizando uma call aqui e já rodo", "Jonas", LocalTime.of(15, 30), false, chatName));
            mensagensFixas.add(new Mensagem("Fechou. Se passar tudo, a gente já manda pra homologação", "Você", LocalTime.of(15, 32), true, chatName));
        } else {
            mensagensFixas.add(new Mensagem("Eae, tranquilo?", chatName, LocalTime.of(22, 20), false, chatName));
            mensagensFixas.add(new Mensagem("Tô precisando de um logo novo pro grupo", chatName, LocalTime.of(22, 21), false, chatName));
            mensagensFixas.add(new Mensagem("Algo mais direto, nada exagerado", chatName, LocalTime.of(22, 22), false, chatName));
            mensagensFixas.add(new Mensagem("Tranquilo. Semana que vem começo. Manda os docs hj", "Você", LocalTime.of(22, 23), true, chatName));
            mensagensFixas.add(new Mensagem("Fechou, valeu", chatName, LocalTime.of(22, 24), false, chatName));
        }

        // combina mensagens fictícias com as mensagens reais
        List<Mensagem> mensagensDoChat = mensagensPorChat.getOrDefault(chatName, new ArrayList<>());
        mensagensFixas.addAll(mensagensDoChat);

        // encontra a última mensagem enviada por você
        int ultimaMensagemDoUsuario = -1;
        for (int i = mensagensFixas.size() - 1; i >= 0; i--) {
            if ("Você".equals(mensagensFixas.get(i).getRemetente())) {
                ultimaMensagemDoUsuario = i;
                break;
            }
        }

        // verifica se as mensagens foram vistas
        boolean mensagemFoiVista = usuarios.stream()
                .filter(u -> !u.getNome().equals("Você"))
                .anyMatch(UsuarioChat::isVisualizouMensagem);

        boolean isGroupChat = chatName.equals("Amigos Faculdade") || chatName.equals("Trabalho");

        // exibe cada mensagem no chat
        for (int i = 0; i < mensagensFixas.size(); i++) {
            Mensagem m = mensagensFixas.get(i);
            boolean ehUsuario = "Você".equals(m.getRemetente());

            // painel pra cada mensagem
            JPanel mensagemPanel = new JPanel();
            mensagemPanel.setLayout(new FlowLayout(ehUsuario ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
            mensagemPanel.setOpaque(false);

            JPanel conteudoPanel = new JPanel();
            conteudoPanel.setLayout(new BoxLayout(conteudoPanel, BoxLayout.Y_AXIS));
            conteudoPanel.setOpaque(false);
            int maxWidth = Math.min(Math.max((int) (frame.getWidth() * 0.55), 300), 600);
            conteudoPanel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));

            // em grupos, mostra o avatar e nome do remetente
            if (isGroupChat && !ehUsuario) {
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                headerPanel.setOpaque(false);

                String remetente = m.getRemetente();
                String avatarPath = remetenteImageMap.getOrDefault(remetente, "/profile/default.jpeg");
                Image avatarImage = null;

                try {
                    URL resourceUrl = getClass().getResource(avatarPath);
                    if (resourceUrl == null) {
                        throw new IllegalArgumentException("Resource not found: " + avatarPath);
                    }
                    ImageIcon avatarIcon = new ImageIcon(resourceUrl);
                    if (avatarIcon.getImage() != null) {
                        avatarImage = getScaledImage(avatarIcon.getImage(), 36, 36);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao carregar imagem para " + remetente + ": " + avatarPath);
                    e.printStackTrace();
                }

                RoundedAvatarLabel avatarLabel = new RoundedAvatarLabel(avatarImage, 36);
                headerPanel.add(avatarLabel);

                JLabel nomeLabel = new JLabel(remetente);
                nomeLabel.setFont(FONT_SUBTITULO);
                nomeLabel.setForeground(new Color(50, 50, 50));
                headerPanel.add(nomeLabel);

                conteudoPanel.add(headerPanel);
            }

            // painel com o texto da mensagem
            JPanel textoPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    // desenha a bolha da mensagem com bordas arredondadas
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(ehUsuario ? COR_MENSAGEM_USUARIO : COR_MENSAGEM_OUTRO);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2d.setColor(COR_SOMBRA);
                    g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 20, 20);
                    g2d.setColor(ehUsuario ? COR_MENSAGEM_USUARIO : COR_MENSAGEM_OUTRO);
                    g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 20, 20);
                }
            };
            textoPanel.setOpaque(false);
            textoPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

            // quebra o texto da mensagem pra caber na tela
            FontMetrics metrics = textoPanel.getFontMetrics(FONT_MENSAGEM);
            String textoQuebrado = quebrarTexto(m.getTexto(), textoPanel.getGraphics(), metrics, maxWidth - 30);
            JLabel textoLabel = new JLabel(textoQuebrado);
            textoLabel.setFont(FONT_MENSAGEM);
            textoLabel.setForeground(ehUsuario ? Color.WHITE : new Color(30, 30, 30));
            textoLabel.setHorizontalAlignment(ehUsuario ? SwingConstants.RIGHT : SwingConstants.LEFT);
            textoLabel.setVerticalAlignment(SwingConstants.TOP);
            textoPanel.add(textoLabel, BorderLayout.CENTER);

            // mostra o horário da mensagem
            String horarioFormatado = m.getHorario().format(formatter);
            JLabel horarioLabel = new JLabel(horarioFormatado);
            horarioLabel.setFont(FONT_SUBTITULO);
            horarioLabel.setForeground(ehUsuario ? new Color(200, 200, 200) : new Color(120, 120, 120));
            horarioLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            textoPanel.add(horarioLabel, BorderLayout.SOUTH);

            conteudoPanel.add(textoPanel);
            mensagemPanel.add(conteudoPanel);

            // mostra "visto" se a última mensagem sua foi visualizada
            if (i == ultimaMensagemDoUsuario && mensagemFoiVista) {
                JPanel vistoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                vistoPanel.setOpaque(false);
                JLabel vistoLabel = new JLabel("Visto");
                vistoLabel.setFont(FONT_SUBTITULO);
                vistoLabel.setForeground(new Color(100, 200, 100));
                vistoPanel.add(vistoLabel);
                areaMensagens.add(vistoPanel);
            }

            areaMensagens.add(mensagemPanel);
            if (i < mensagensFixas.size() - 1) {
                areaMensagens.add(Box.createVerticalStrut(25));
            }
        }

        areaMensagens.revalidate();
        areaMensagens.repaint();

        // rola a tela pro final das mensagens
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) areaMensagens.getParent().getParent();
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    // N/T que inferno fazer essa quebra de texto manual html no meio do java, parabéns ao infeliz que bolou isso
    private String quebrarTexto(String texto, Graphics g, FontMetrics metrics, int maxWidth) {
        StringBuilder resultado = new StringBuilder("<html>");
        StringBuilder linhaAtual = new StringBuilder();
        int larguraAtual = 0;

        String[] linhas = texto.split("\n");
        for (int i = 0; i < linhas.length; i++) {
            String[] palavras = linhas[i].trim().split("\\s+");
            linhaAtual.setLength(0);
            larguraAtual = 0;

            for (String palavra : palavras) {
                int larguraPalavra = metrics.stringWidth(palavra + " ");
                if (larguraAtual + larguraPalavra > maxWidth && linhaAtual.length() > 0) {
                    resultado.append(linhaAtual.toString().trim()).append("<br>");
                    linhaAtual.setLength(0);
                    larguraAtual = 0;
                }
                linhaAtual.append(palavra).append(" ");
                larguraAtual += larguraPalavra;
            }

            if (linhaAtual.length() > 0) {
                resultado.append(linhaAtual.toString().trim());
            }
            if (i < linhas.length - 1) {
                resultado.append("<br>");
            }
        }

        resultado.append("</html>");
        return resultado.toString();
    }

    // cria um painel pro diálogo de permissão, aquele que pergunta se você deixa o app enviar mensagens
    private int permissaoDialog() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                // desenha o fundo do diálogo com bordas arredondadas e cor branca
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(COR_SECUNDARIA);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // adiciona um espaço ao redor do conteúdo

        // texto do diálogo, pedindo permissão pra enviar mensagens
        JLabel messageLabel = new JLabel("<html>Este aplicativo precisa de permissão para enviar mensagens.<br>Deseja permitir?</html>");
        messageLabel.setFont(FONT_SUBTITULO); // usa a fonte de subtítulo
        messageLabel.setForeground(new Color(30, 30, 30)); // cor do texto: quase preto
        panel.add(messageLabel, BorderLayout.CENTER); // coloca o texto no centro do painel

        // configura o estilo dos botões do diálogo (sim/não)
        UIManager.put("Button.background", COR_PRIMARIA); // fundo azul vibrante
        UIManager.put("Button.foreground", Color.WHITE); // texto branco
        UIManager.put("Button.font", FONT_SUBTITULO); // fonte de subtítulo
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(12, 20, 12, 20)); // espaçamento interno
        UIManager.put("Button.select", new Color(0, 100, 220)); // cor quando o botão é selecionado

        // cria o diálogo com opções "sim" e "não".
        JOptionPane optionPane = new JOptionPane(
                panel,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[]{"Sim", "Não"},
                "Sim"
        );
        JDialog dialog = optionPane.createDialog(frame, "Permissão Necessária"); // título do diálogo
        dialog.getContentPane().setBackground(COR_SECUNDARIA); // fundo branco pro diálogo
        dialog.setVisible(true); // mostra o diálogo

// retorna a escolha do usuário (sim ou não). se não escolher, assume "não"
        Object value = optionPane.getValue();
        if (value == null || value.equals("Não")) {
            return JOptionPane.NO_OPTION; // retorna 1 (equivalente a "não")
        } else if (value.equals("Sim")) {
            return JOptionPane.YES_OPTION; // retorna 0 (equivalente a "sim")
        } else {
            return JOptionPane.NO_OPTION; // caso padrão, assume "não"
        }
    }

    // método da interface aplicativo que notifica quando tem uma mensagem nova
    @Override
    public void notifica() {
        // manda uma notificação pra todos os observadores (outros usuários) dizendo que tem mensagem nova no chat atual
        observable.notificarObservers("Nova mensagem no chat " + currentChatName);
    }

    // método da interface `aplicativo` que verifica e pede permissão pro usuário
    @Override
    public void permissao() {
        // procura o usuário "você" na lista de usuários
        UsuarioChat usuarioAtual = usuarios.stream()
                .filter(u -> u.getNome().equals("Você"))
                .findFirst()
                .orElse(null);
        // se encontrou o usuário e ele não tem permissão, mostra o diálogo de permissão
        if (usuarioAtual != null && !usuarioAtual.temPermissao()) {
            int resposta = permissaoDialog();
            // se o usuário clicar em "sim", dá permissão pra ele
            if (resposta == JOptionPane.YES_OPTION) {
                usuarioAtual.permite();
            }
        }
    }

    // método da interface aplicativo que atualiza o armazenamento de todos os usuários
    @Override
    public void armazenamento() {
        // chama o método armazena() pra cada usuário, atualizando o uso de armazenamento
        for (UsuarioChat u : usuarios) {
            u.armazena();
        }
    }

    // método da interface aplicativo que atualiza o uso de dados de todos os usuários
    @Override
    public void usoDeDados() {
        // chama o método usadados() pra cada usuário, atualizando o uso de dados
        for (UsuarioChat u : usuarios) {
            u.usaDados();
        }
    }
}