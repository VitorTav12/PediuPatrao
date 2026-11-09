package com.umc.pediupatrao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.umc.pediupatrao.entity.Perfil;
import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {

    private static final Logger loggerApp = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PedidoService servicoPedido;

    @Autowired
    private ClienteService servicoCliente;

    @Autowired
    private ProdutoService servicoProduto;

    @Autowired
    private UsuarioService servicoUtilizador;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model modelo) {
        int horaAtual = LocalTime.now().getHour();
        String mensagemSaudacao;
        
        if (horaAtual >= 6 && horaAtual < 12) {
            mensagemSaudacao = "Bom dia! Pronto para gerir os pedidos de hoje?";
        } else if (horaAtual >= 12 && horaAtual < 18) {
            mensagemSaudacao = "Boa tarde! O movimento está a todo vapor.";
        } else {
            mensagemSaudacao = "Boa noite! Vamos fechar o caixa com chave de ouro.";
        }

        long contagemProdutos = servicoProduto.listarProdutos().size();
        long contagemPedidos = servicoPedido.listarPedidos().size();

        modelo.addAttribute("saudacaoDinamica", mensagemSaudacao);
        modelo.addAttribute("totalProdutos", contagemProdutos);
        modelo.addAttribute("totalPedidos", contagemPedidos);
        modelo.addAttribute("content", "home :: content");
        return "layout";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model modelo) {
        List<Usuario> listaUtilizadores = (List<Usuario>) servicoUtilizador.listarTodos();
        modelo.addAttribute("usuarios", listaUtilizadores);
        modelo.addAttribute("content", "usuarios/lista :: content");
        loggerApp.info("A carregar fragmento: usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model modelo) {
        modelo.addAttribute("usuario", new Usuario());
        modelo.addAttribute("perfis", Perfil.values());
        modelo.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model modelo, RedirectAttributes atributosRedirecionamento) {
        boolean localizado = servicoUtilizador.buscarPorId(id).map(u -> {
            modelo.addAttribute("usuario", u);
            return true;
        }).orElse(false);

        if (!localizado) {
            atributosRedirecionamento.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        modelo.addAttribute("perfis", Perfil.values());
        modelo.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute Usuario dadosUsuario, RedirectAttributes atributosRedirecionamento) {
        try {
            servicoUtilizador.salvarUsuario(dadosUsuario);
            atributosRedirecionamento.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        } catch (IllegalArgumentException ex) {
            atributosRedirecionamento.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String atualizarUsuario(@PathVariable String id, @ModelAttribute Usuario dadosUsuario, Principal principalLogado, RedirectAttributes atributosRedirecionamento) {
        try {
            servicoUtilizador.atualizarUsuario(id, dadosUsuario, principalLogado.getName());
            atributosRedirecionamento.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        } catch (IllegalArgumentException ex) {
            atributosRedirecionamento.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    public String deletarUsuario(@PathVariable String id, Principal principalLogado, RedirectAttributes atributosRedirecionamento) {
        try {
            servicoUtilizador.deletarUsuario(id, principalLogado.getName());
            atributosRedirecionamento.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (IllegalArgumentException ex) {
            atributosRedirecionamento.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/pedidos")
    public String pedidos(Model modelo, Authentication authContexto) {
        List<Pedido> listaPedidos = servicoPedido.listarPedidos();

        Map<String, String> mapaNomesClientes = servicoCliente.listarClientes()
                .stream()
                .collect(Collectors.toMap(
                        Cliente::getId,
                        Cliente::getNome,
                        (nomeExistente, nomeNovo) -> nomeExistente
                ));

        modelo.addAttribute("pedidos", listaPedidos);
        modelo.addAttribute("nomesClientes", mapaNomesClientes);
        modelo.addAttribute("ehGerente", authContexto != null && authContexto.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE")));
        modelo.addAttribute("content", "pedidos :: content");
        loggerApp.info("A carregar fragmento: pedidos :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    public String novoPedido(Model modelo) {
        List<Cliente> listaClientes = servicoCliente.listarClientes();
        List<Produto> listaProdutos = servicoProduto.listarProdutos();

        modelo.addAttribute("clientes", listaClientes);
        modelo.addAttribute("produtos", listaProdutos);
        modelo.addAttribute("content", "pedidos/novo-pedido :: content");

        return "layout";
    }

    @GetMapping("/produtos")
    public String produtos(Model modelo) {
        List<Produto> listaProdutos = servicoProduto.listarProdutos();
        modelo.addAttribute("produtos", listaProdutos);
        modelo.addAttribute("content", "produtos/lista :: content");
        loggerApp.info("A carregar fragmento: produtos/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes")
    public String clientes(Model modelo) {
        List<Cliente> listaClientes = servicoCliente.listarClientes();
        modelo.addAttribute("clientes", listaClientes);
        modelo.addAttribute("content", "clientes/lista :: content");
        loggerApp.info("A carregar fragmento: clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model modelo) {
        modelo.addAttribute("cliente", new Cliente());
        modelo.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute Cliente dadosCliente, Authentication authContexto, RedirectAttributes atributosRedirecionamento) {
        try {
            servicoCliente.salvar(dadosCliente, authContexto);
            atributosRedirecionamento.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        } catch (IllegalArgumentException | SecurityException ex) {
            atributosRedirecionamento.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model modelo, RedirectAttributes atributosRedirecionamento) {
        boolean localizado = servicoCliente.buscarPorId(id).map(c -> {
            modelo.addAttribute("cliente", c);
            return true;
        }).orElse(false);

        if (!localizado) {
            atributosRedirecionamento.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }

        modelo.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute Cliente dadosCliente, Authentication authContexto, RedirectAttributes atributosRedirecionamento) {
        try {
            dadosCliente.setId(id);
            servicoCliente.salvar(dadosCliente, authContexto);
            atributosRedirecionamento.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        } catch (IllegalArgumentException | SecurityException ex) {
            atributosRedirecionamento.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model modelo) {
        modelo.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    @GetMapping("/teste")
    public String teste() {
        return "teste";
    }

    @GetMapping("/admin")
    public String admin(Model modelo) {
        modelo.addAttribute("content", "admin :: content");
        loggerApp.info("A carregar fragmento: admin :: content");
        return "layout";
    }
}