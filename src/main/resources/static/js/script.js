document.querySelector('.toggle-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('active');
});

function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}

function montarCabecalhos(comJson) {
    const cabecalhos = {};

    if (comJson) {
        cabecalhos["Content-Type"] = "application/json";
    }

    const cookieCsrf = document.cookie
        .split("; ")
        .find(cookie => cookie.startsWith("XSRF-TOKEN="));

    if (cookieCsrf) {
        cabecalhos["X-XSRF-TOKEN"] = decodeURIComponent(
            cookieCsrf.substring("XSRF-TOKEN=".length)
        );
    }

    return cabecalhos;
}

document.addEventListener("DOMContentLoaded", () => {
    const produtoForm = document.getElementById("produtoForm");

    if (!produtoForm) return;

    const campoId = document.getElementById("produtoId");
    const tituloForm = document.getElementById("tituloFormProduto");
    const botaoCancelarEdicao = document.getElementById("cancelarEdicaoProduto");
    const botaoNovo = document.getElementById("botaoNovoProduto");
    const painel = document.getElementById("formularioProduto");

    function limparFormulario() {
        produtoForm.reset();
        campoId.value = "";
        tituloForm.textContent = "Novo produto";
        botaoCancelarEdicao.classList.add("d-none");
    }

    botaoNovo.addEventListener("click", limparFormulario);

    botaoCancelarEdicao.addEventListener("click", () => {
        limparFormulario();
        window.bootstrap.Collapse.getOrCreateInstance(painel, { toggle: false }).hide();
    });

    document.querySelectorAll(".btn-editar").forEach(botao => {
        botao.addEventListener("click", () => {
            campoId.value = botao.dataset.id;
            document.getElementById("nome").value = botao.dataset.nome || "";
            document.getElementById("tipo").value = botao.dataset.tipo || "";
            document.getElementById("preco").value = botao.dataset.preco || "";
            document.getElementById("ativo").checked = botao.dataset.ativo === "true";
            tituloForm.textContent = "Editar produto";
            botaoCancelarEdicao.classList.remove("d-none");
            window.bootstrap.Collapse.getOrCreateInstance(painel, { toggle: false }).show();
            painel.scrollIntoView({ behavior: "smooth" });
        });
    });

    produtoForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const produto = {
            nome: document.getElementById("nome").value.trim(),
            tipo: document.getElementById("tipo").value,
            preco: Number(document.getElementById("preco").value),
            ativo: document.getElementById("ativo").checked
        };

        const id = campoId.value;
        const url = id ? `/api/produtos/${id}` : "/api/produtos";
        const metodo = id ? "PUT" : "POST";

        try {
            const response = await fetch(url, {
                method: metodo,
                headers: montarCabecalhos(true),
                body: JSON.stringify(produto)
            });

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            alert(id ? "Produto atualizado com sucesso!" : "Produto cadastrado com sucesso!");
            window.location.reload();
        } catch (error) {
            console.error("Erro ao salvar produto:", error);
            alert("Não foi possível salvar o produto. Verifique o console.");
        }
    });
});

document.addEventListener("click", async (event) => {
    const botao = event.target.closest(".btn-excluir");

    if (!botao) return;

    const id = botao.dataset.id;

    if (!confirm("Tem certeza que deseja excluir este produto?")) {
        return;
    }

    try {
        const response = await fetch(`/api/produtos/${id}`, {
            method: "DELETE",
            headers: montarCabecalhos(false)
        });

        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }

        alert("Produto excluído com sucesso!");
        window.location.reload();
    } catch (error) {
        console.error("Erro ao excluir produto:", error);
        alert("Não foi possível excluir o produto. Verifique o console.");
    }
});