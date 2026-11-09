package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private LogService logService;

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    public Cliente salvar(Cliente cliente, Authentication authentication) {
        if (cliente.getId() == null) {
            Cliente clienteSalvo = clienteRepository.save(cliente);

            logService.registrar(
                    authentication,
                    "CRIACAO_CLIENTE",
                    "Cliente",
                    clienteSalvo.getId(),
                    compararCampos(null, clienteSalvo),
                    null
            );

            return clienteSalvo;
        }

        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado para atualização."
                ));

        List<Log.DetalheAlteracao> alteracoes = compararCampos(anterior, cliente);

        Cliente atualizado = clienteRepository.save(cliente);

        if (!alteracoes.isEmpty()) {
            logService.registrar(
                    authentication,
                    "ALTERACAO_CLIENTE",
                    "Cliente",
                    atualizado.getId(),
                    alteracoes,
                    null
            );
        }

        return atualizado;
    }

    public void excluir(String id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("É necessário identificar o usuário para excluir um cliente.");
        }

        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado para exclusão."
                ));

        List<Log.DetalheAlteracao> alteracoes = compararCampos(existente, null);

        clienteRepository.deleteById(id);

        logService.registrar(
                authentication,
                "EXCLUSAO_CLIENTE",
                "Cliente",
                id,
                alteracoes,
                null
        );
    }

    private List<Log.DetalheAlteracao> compararCampos(Cliente anterior, Cliente novo) {
        List<Log.DetalheAlteracao> alteracoes = new ArrayList<>();

        adicionarAlteracao(alteracoes, "nome", valor(anterior, Cliente::getNome), valor(novo, Cliente::getNome));
        adicionarAlteracao(alteracoes, "telefone", valor(anterior, Cliente::getTelefone), valor(novo, Cliente::getTelefone));
        adicionarAlteracao(alteracoes, "cep", valor(anterior, Cliente::getCep), valor(novo, Cliente::getCep));
        adicionarAlteracao(alteracoes, "logradouro", valor(anterior, Cliente::getLogradouro), valor(novo, Cliente::getLogradouro));
        adicionarAlteracao(alteracoes, "numero", valor(anterior, Cliente::getNumero), valor(novo, Cliente::getNumero));
        adicionarAlteracao(alteracoes, "complemento", valor(anterior, Cliente::getComplemento), valor(novo, Cliente::getComplemento));
        adicionarAlteracao(alteracoes, "bairro", valor(anterior, Cliente::getBairro), valor(novo, Cliente::getBairro));
        adicionarAlteracao(alteracoes, "cidade", valor(anterior, Cliente::getCidade), valor(novo, Cliente::getCidade));
        adicionarAlteracao(alteracoes, "estado", valor(anterior, Cliente::getEstado), valor(novo, Cliente::getEstado));
        adicionarAlteracao(alteracoes, "endereco", valor(anterior, Cliente::getEndereco), valor(novo, Cliente::getEndereco));

        return alteracoes;
    }

    private String valor(Cliente cliente, Function<Cliente, String> campo) {
        return cliente == null ? null : campo.apply(cliente);
    }

    private void adicionarAlteracao(
            List<Log.DetalheAlteracao> alteracoes,
            String campo,
            String valorAnterior,
            String valorNovo) {

        if (!Objects.equals(valorAnterior, valorNovo)) {
            alteracoes.add(
                    new Log.DetalheAlteracao(campo, valorAnterior, valorNovo)
            );
        }
    }
}