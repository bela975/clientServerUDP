#  Projeto de Aplicação Cliente-Servidor com UDP
## Objetivo Geral:
Desenvolver uma aplicação, em Java, cliente-servidor que, utilizando o protocolo UDP, ofereça um transporte confiável de dados em um canal que pode apresentar perdas de dados e erros.

## Funcionalidades que a Aplicação deve Garantir: 
- Soma de verificação (Checksum): Para detectar erros nos pacotes recebidos.
- Temporizador (Timer): Para retransmissão de pacotes que não foram reconhecidos em um tempo esperado.
- Número de sequência: Para controle de ordenação dos pacotes.
- Reconhecimento (ACK): Para informar o remetente do recebimento de pacotes.
- Reconhecimento negativo (NACK): Para informar o remetente da não recepção de pacotes ou recepção com erro.
- Janela e paralelismo: Para controle de fluxo e envio múltiplo de pacotes.
- Simulação de Falhas
- Integridade e perdas de mensagens
- Envio de Pacotes
