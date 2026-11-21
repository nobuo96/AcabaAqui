package com.acabaaqui.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class LifecycleStatusTransitionPolicy {

    public static final String SOLICITACAO = "SOLICITACAO";
    public static final String OPORTUNIDADE = "OPORTUNIDADE";
    public static final String VISITA = "VISITA";
    public static final String ORCAMENTO = "ORCAMENTO";
    public static final String PAGAMENTO = "PAGAMENTO";
    public static final String EXECUCAO = "EXECUCAO";
    public static final String DISPUTA = "DISPUTA";
    public static final String GARANTIA = "GARANTIA";

    public static final String CLIENTE = "CLIENTE";
    public static final String PRESTADOR = "PRESTADOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String SISTEMA = "SISTEMA";
    public static final String MERCADO_PAGO = "MERCADO_PAGO";

    private static final Map<Transition, Set<String>> ALLOWED_TRANSITIONS = createTransitions();

    private LifecycleStatusTransitionPolicy() {
    }

    public static void validar(
            String entityType,
            String previousStatus,
            String newStatus,
            String actorType) {
        Set<String> allowedActors = ALLOWED_TRANSITIONS.get(
                new Transition(entityType, previousStatus, newStatus));
        if (allowedActors == null || !allowedActors.contains(actorType)) {
            throw new IllegalStateException("Transição de status não permitida para "
                    + entityType + ": " + previousStatus + " -> " + newStatus
                    + " por " + actorType + ".");
        }
    }

    private static Map<Transition, Set<String>> createTransitions() {
        Map<Transition, Set<String>> transitions = new HashMap<>();

        allow(transitions, SOLICITACAO, null, "CRIADA", CLIENTE);
        allow(transitions, SOLICITACAO, "CRIADA", "ENCERRADA", CLIENTE, SISTEMA);

        allow(transitions, OPORTUNIDADE, null, "PENDENTE", SISTEMA);
        allow(transitions, OPORTUNIDADE, "PENDENTE", "ACEITA", PRESTADOR);
        allow(transitions, OPORTUNIDADE, "PENDENTE", "RECUSADA", PRESTADOR);
        allow(transitions, OPORTUNIDADE, "PENDENTE", "ENCERRADA", CLIENTE, SISTEMA);

        allow(transitions, VISITA, null, "DISPONIVEL", CLIENTE);
        allow(transitions, VISITA, "DISPONIVEL", "RESERVADA", PRESTADOR);
        allow(transitions, VISITA, "RESERVADA", "CONFIRMADA", CLIENTE);
        allow(transitions, VISITA, "RESERVADA", "CANCELADA", CLIENTE, PRESTADOR, SISTEMA);
        allow(transitions, VISITA, "CONFIRMADA", "CANCELADA", CLIENTE, PRESTADOR, SISTEMA);
        allow(transitions, VISITA, "CANCELADA", "DISPONIVEL", SISTEMA);
        allow(transitions, VISITA, "DISPONIVEL", "CANCELADA", CLIENTE, SISTEMA);
        allow(transitions, VISITA, "DISPONIVEL", "EXPIRADA", SISTEMA);
        allow(transitions, VISITA, "RESERVADA", "EXPIRADA", SISTEMA);
        allow(transitions, VISITA, "CONFIRMADA", "EXPIRADA", SISTEMA);

        allow(transitions, ORCAMENTO, null, "ENVIADO", PRESTADOR);
        allow(transitions, ORCAMENTO, "ENVIADO", "APROVADO", CLIENTE);
        allow(transitions, ORCAMENTO, "ENVIADO", "RECUSADO", CLIENTE);
        allow(transitions, ORCAMENTO, "ENVIADO", "EXPIRADO", SISTEMA);
        allow(transitions, ORCAMENTO, "ENVIADO", "CANCELADO", CLIENTE, SISTEMA);
        allow(transitions, ORCAMENTO, "APROVADO", "CANCELADO", CLIENTE, SISTEMA);

        allow(transitions, PAGAMENTO, null, "AGUARDANDO_PAGAMENTO", SISTEMA);
        allow(transitions, PAGAMENTO, "AGUARDANDO_PAGAMENTO", "PAGO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "AGUARDANDO_PAGAMENTO", "RECUSADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "AGUARDANDO_PAGAMENTO", "EXPIRADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "AGUARDANDO_PAGAMENTO", "CANCELADO", CLIENTE, SISTEMA, MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "AGUARDANDO_PAGAMENTO", "CONTESTADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "RECUSADO", "AGUARDANDO_PAGAMENTO", CLIENTE);
        allow(transitions, PAGAMENTO, "EXPIRADO", "AGUARDANDO_PAGAMENTO", CLIENTE);
        allow(transitions, PAGAMENTO, "CANCELADO", "AGUARDANDO_PAGAMENTO", CLIENTE);
        allow(transitions, PAGAMENTO, "PAGO", "REEMBOLSO_PENDENTE",
                CLIENTE, ADMINISTRADOR, SISTEMA, MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REEMBOLSO_PENDENTE", "REEMBOLSADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REEMBOLSO_PENDENTE", "PAGO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REEMBOLSO_PENDENTE", "CONTESTADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "PAGO", "REEMBOLSADO_PARCIAL", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REEMBOLSO_PENDENTE", "REEMBOLSADO_PARCIAL", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "PAGO", "CONTESTADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REEMBOLSADO_PARCIAL", "CONTESTADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "PAGO", "REPASSE_PENDENTE", SISTEMA);
        allow(transitions, PAGAMENTO, "REPASSE_PENDENTE", "REPASSADO", ADMINISTRADOR);
        allow(transitions, PAGAMENTO, "REPASSE_PENDENTE", "REEMBOLSO_PENDENTE",
                ADMINISTRADOR, MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REPASSE_PENDENTE", "REEMBOLSADO_PARCIAL", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REPASSE_PENDENTE", "CONTESTADO", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REPASSADO", "REEMBOLSO_PENDENTE", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REPASSADO", "REEMBOLSADO_PARCIAL", MERCADO_PAGO);
        allow(transitions, PAGAMENTO, "REPASSADO", "CONTESTADO", MERCADO_PAGO);

        allow(transitions, EXECUCAO, null, "AGUARDANDO_INICIO", SISTEMA);
        allow(transitions, EXECUCAO, "AGUARDANDO_INICIO", "EM_ANDAMENTO", PRESTADOR);
        allow(transitions, EXECUCAO, "AGUARDANDO_INICIO", "CANCELADA", CLIENTE);
        allow(transitions, EXECUCAO, "EM_ANDAMENTO", "CONTESTADA", CLIENTE);
        allow(transitions, EXECUCAO, "EM_ANDAMENTO", "CONCLUIDA_PELO_PRESTADOR", PRESTADOR);
        allow(transitions, EXECUCAO, "CONCLUIDA_PELO_PRESTADOR", "APROVADA", CLIENTE, SISTEMA);
        allow(transitions, EXECUCAO, "CONCLUIDA_PELO_PRESTADOR", "CONTESTADA", CLIENTE);
        allow(transitions, EXECUCAO, "CONTESTADA", "APROVADA", ADMINISTRADOR);

        allow(transitions, DISPUTA, null, "ABERTA", CLIENTE);
        allow(transitions, DISPUTA, "ABERTA", "ENCERRADA_REEMBOLSO", ADMINISTRADOR);
        allow(transitions, DISPUTA, "ABERTA", "ENCERRADA_REPASSE", ADMINISTRADOR);

        allow(transitions, GARANTIA, null, "ATIVA", SISTEMA);
        allow(transitions, GARANTIA, "ATIVA", "ACIONADA", CLIENTE);
        allow(transitions, GARANTIA, "ACIONADA", "CORRIGIDA", PRESTADOR);
        allow(transitions, GARANTIA, "ACIONADA", "REEMBOLSO_PENDENTE", SISTEMA, ADMINISTRADOR);

        return Map.copyOf(transitions);
    }

    private static void allow(
            Map<Transition, Set<String>> transitions,
            String entityType,
            String previousStatus,
            String newStatus,
            String... actors) {
        transitions.put(new Transition(entityType, previousStatus, newStatus), Set.of(actors));
    }

    private record Transition(String entityType, String previousStatus, String newStatus) {
    }
}
