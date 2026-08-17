import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const tentativas = Number(__ENV.TENTATIVAS || 2000);
const confirmadas = new Counter('inscricoes_confirmadas');
const listaEspera = new Counter('inscricoes_lista_espera');
const respostasInesperadas = new Counter('respostas_inesperadas');

export const options = {
  scenarios: {
    inscricoes_diretas: {
      executor: 'per-vu-iterations',
      vus: tentativas,
      iterations: 1,
      maxDuration: '10m',
      gracefulStop: '0s',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate<0.001'],
    respostas_inesperadas: ['count==0'],
    'http_req_duration{cenario:inscricao_direta}': ['p(95)<120000', 'p(99)<180000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const corpo = JSON.stringify({
    nome: `Participante de carga ${__VU}`,
    email: `carga.${__ENV.EXECUCAO}.${__VU}@example.com`,
  });
  const resposta = http.post(
    `${__ENV.BASE_URL}/api/v1/publico/eventos/${__ENV.EVENTO_SLUG}/inscricoes`,
    corpo,
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { cenario: 'inscricao_direta' },
      timeout: '5m',
    },
  );

  let json = null;
  try {
    json = resposta.json();
  } catch (_) {
    respostasInesperadas.add(1);
  }

  const respostaValida = check(resposta, {
    'inscricao retorna 201': (resultado) => resultado.status === 201,
    'status de negocio reconhecido': () =>
      json !== null && (json.status === 'CONFIRMADA' || json.status === 'LISTA_ESPERA'),
  });
  if (!respostaValida || json === null) {
    if (__VU <= 50) {
      console.error(`VU ${__VU} recebeu HTTP ${resposta.status}: ${resposta.body}`);
    }
    respostasInesperadas.add(1);
    return;
  }
  if (json.status === 'CONFIRMADA') {
    confirmadas.add(1);
  } else {
    listaEspera.add(1);
  }
}
