# Real-Time Vehicle Tracker 🚌

## 🎯 Objetivo da Aplicação

O objetivo principal da nossa aplicação é explorar tecnologias modernas do ecossistema Spring para criar um serviço de rastreamento de veículos em tempo real. O sistema identifica a localização do usuário a partir do seu endereço IP, busca agências de transporte público próximas e, futuramente, exibirá a posição dos veículos em tempo real.

Este projeto serve como um campo de aprendizado prático para os seguintes conceitos:

- **Programação Reativa e Não-Bloqueante** com Spring WebClient.
- **Comunicação em Tempo Real** com Spring WebSockets.
- **Manipulação de Dados em Diferentes Formatos** (JSON e XML).
- **Integração com APIs Externas** para geolocalização e dados de transporte.
- **Boas Práticas de Arquitetura Java** com Spring Boot.

## 🛠️ Como Funciona: O Fluxo de Dados

O fluxo de dados da nossa aplicação foi pensado para ser eficiente e escalável. Aqui está o passo a passo:

1. **Captura do IP do Usuário**: Assim que uma requisição chega ao nosso endpoint, o sistema extrai o endereço IP do usuário. Em um ambiente de produção, é comum que o IP venha no header `x-forwarded-for`, pois a aplicação geralmente está atrás de um proxy ou load balancer.
2. **Geolocalização**: Com o IP em mãos, fazemos uma chamada a uma API externa (como a `ip-api.com`) para obter a geolocalização do usuário, incluindo sua latitude e longitude.
3. **Busca por Agências de Transporte**: Em seguida, utilizamos a API da NextBus, que fornece dados do sistema de transporte público. A primeira etapa é consultar a lista de agências de transporte (`agencyList`) disponíveis.
4. **Processamento de Dados**: A API da NextBus retorna os dados em formato XML. Nossa aplicação converte essa resposta em objetos Java para que possamos manipulá-los facilmente.
5. **(Próximos Passos) Busca por Veículos Próximos**: Com a localização do usuário e a lista de agências, o próximo passo seria implementar a lógica para cruzar essas informações e encontrar as rotas e os veículos que estão mais próximos do usuário.
6. **(Próximos Passos) Envio em Tempo Real**: Finalmente, a ideia é usar WebSockets para enviar as atualizações da localização dos veículos para o cliente de forma contínua, sem que ele precise fazer novas requisições.
