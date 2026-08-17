import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const inscricoes = new SharedArray('inscricoes confirmadas', () =>
  JSON.parse(open('/results/cancellations.json')),
);
const promocoes = new Counter('promocoes_realizadas');
const respostasInesperadas = new Counter('respostas_inesperadas');

export const options = {
  scenarios: {
    cancelamentos_com_promocao: {
      executor: 'per-vu-iterations',
      vus: inscricoes.length,
      iterations: 1,
      maxDuration: '5m',
      gracefulStop: '0s',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate<0.001'],
    respostas_inesperadas: ['count==0'],
    'http_req_duration{cenario:cancelamento_promocao}': ['p(95)<8000', 'p(99)<10000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const inscricaoId = inscricoes[__VU - 1];
  const resposta = http.post(
    `${__ENV.BASE_URL}/api/v1/inscricoes/${inscricaoId}/cancelamento`,
    null,
    {
      headers: { Authorization: `Bearer ${__ENV.TOKEN_ORGANIZADOR}` },
      tags: { cenario: 'cancelamento_promocao' },
      timeout: '60s',
    },
  );

  let json = null;
  try {
    json = resposta.json();
  } catch (_) {
    respostasInesperadas.add(1);
  }

  const respostaValida = check(resposta, {
    'cancelamento retorna 200': (resultado) => resultado.status === 200,
    'cancelamento promove uma inscricao': () => json !== null && json.inscricaoPromovidaId !== null,
  });
  if (!respostaValida) {
    respostasInesperadas.add(1);
    return;
  }
  promocoes.add(1);
}
