# Real-Time Vehicle Tracker 🚌

## 🎯 Objetivo da Aplicação

O objetivo principal da nossa aplicação é explorar tecnologias modernas do ecossistema Spring para criar um serviço de rastreamento de veículos em tempo real. O sistema identifica a geolocalização do usuário, busca agências de transporte e então permite ao usuário selecionar agências para visualizar informações sobre suas rotas em tempo-real.

Este projeto serve como um campo de aprendizado prático para os seguintes conceitos:

- **Programação Reativa e Não-Bloqueante** com Spring WebClient (Reactor).
- **Comunicação em Tempo Real** com Spring WebSockets.
- **Manipulação de Dados em Diferentes Formatos** (JSON e XML).
- **Integração com APIs Externas** para geolocalização e dados de transporte.
- **Boas Práticas de Arquitetura Java** com Spring Boot.

## 🛠️ Como Funciona: O Fluxo de Dados

O fluxo de dados da aplicação foi pensado para ser eficiente e escalável. Aqui está o passo a passo:

### Antes

1. **Captura do IP do Usuário**: Assim que uma requisição chega ao nosso endpoint, o sistema extrai o endereço IP do usuário. Em um ambiente de produção, é comum que o IP venha no header `x-forwarded-for`, pois a aplicação geralmente está atrás de um proxy ou load balancer.
2. **Geolocalização**: Com o IP em mãos, fazemos uma chamada a uma API externa (como a `ip-api.com`) para obter a geolocalização do usuário, incluindo sua latitude e longitude.

### Agora

1. **Geolocalização do Usuário**: A geolocalização do usuário é feita através da API navigator.geolocation, que tenta obter a posição via GPS e, como fallback, usa Wi-Fi ou rede móvel.

   ```javascript
   window.onload = function () {
    if (navigator.geolocation) {
     navigator.geolocation.getCurrentPosition(success, error);
    } else {
     alert('Geolocation is not supported by this browser.');
    }
   };
   ```

2. **Busca por Agências de Transporte**: Em seguida, utilizamos a API da NextBus, que fornece dados do sistema de transporte público. A primeira etapa é consultar a lista de agências de transporte (`agencyList`) disponíveis.
3. **Cacheamento de Agências**: Assim que obtemos a lista de agências, já que esta informação não é uma informação que tem mudanças frequentes, armazenamos essa lista no cache usando o Spring Cache com o Caffeine como provedor, reduzindo o tempo de resposta da requisição para ~8ms. Para utilizar o Caffeine como CacheManager do Spring Cache, é necessário criar um Bean de CacheManager, para isso, criei uma classe de Configuration CacheConfig onde crio e configuro esse bean, definindo o nome do espaço reservado para este cache (agencies), o tempo de expiração do cache (3 horas nesse caso) e o tamanho máximo de dados deste cache (200). Como é uma aplicação reativa (usando Reactor) também é necessário definir o modo do cache para assíncrono.

   ```java
   @Configuration
   public class CacheConfig {

       @Bean
       public CacheManager cacheManager() {
           CaffeineCacheManager cacheManager = new CaffeineCacheManager("agencies");
           cacheManager.setCaffeine(Caffeine.newBuilder()
                   .expireAfterWrite(3, TimeUnit.HOURS)
                   .maximumSize(200));

           cacheManager.setAsyncCacheMode(true);
           cacheManager.setAllowNullValues(false);

           return cacheManager;
       }
   }

   ```

4. **Processamento de Dados**: A API da NextBus retorna os dados em formato XML. Nossa aplicação converte essa resposta em objetos Java para que possamos manipulá-los facilmente. Essa conversão é feita utilizando o XmlMapper da API jackson dataformat. Como faço a conversão em vários momentos diferentes, optei por criar um método (numa classe XmlUtils) de conversão genérico, que recebe como paramêtros: Uma String (o XML em si) e um class token (ou seja: T é o tipo genérico da classe, e `Class<T>` é o objeto que representa o tipo da classe em tempo de execução), dessa forma posso utilizar o XmlMapper para converter a String recebida como parâmetro num objeto do mesmo tipo do class token recebido como paramêtro, assim posso reutilizar esse método de conversão em todo o projeto. Como o objeto XmlMapper terá sempre a mesma configuração, optei por cria-lo como um singleton, para evitar ficar recriando-o sempre que chamar o método doXmlMapping.

   ```java
   public class XmlUtils {

       private static final XmlMapper XML_MAPPER = XmlMapper.builder()
               .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
               .configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, true)
               .build();

       static {
           XML_MAPPER.registerModule(new ParameterNamesModule());
       }

       /**
        * Map a string containing a xml object to the class received as a parameter
        *
        * @param <T>
        * @param xmlString
        * @param desiredClass
        * @return <T>.class
        */
       public static <T> T doXmlMapping(String xmlString, Class<T> desiredClass) {
           try {
               return XML_MAPPER.readValue(xmlString, desiredClass);
           } catch (JsonProcessingException e) {
               throw new RuntimeException(e);
           }
       }
   }
   ```

5. **Consulta das Rotas de uma determinada agência**: O usuário pode selecionar uma agência (agency tag) para visualizar suas rotas. Com a agency tag, é retornado uma lista de rotas dessa agência (em formato XML, então aqui é retornado esse XML em formato de string, fazemos a conversão para um objeto RouteList _ver passo 4_).
6. **Enriquecendo os dados das Rotas**: Prosseguimos expandindo essa lista (usando flatMapMany) e convertemos o resultado (`Mono<RouteList>`) em um `Flux<Route>` (explicação para Mono e Flux no fim). Para processar cada rota individualmente e usamos o flatMap para processar cada objeto Route da lista de rotas, realizando o seguinte processo para cada Route:

   1. Buscamos as Predictions de uma Route usando a agency tag e a tag das Stops da Route
   2. Acrescentamos esses dados num Mono de RouteResponse, que nada mais é do que: A Route enriquecida com os dados de Predictions.
   3. Em seguida, novamente processamos cada Route, agora para buscar os Vehicles que estão percorrendo a Route (usando a agency tag e a route tag).
   4. Retornamos o RouteResponse final enriquecido com: os dados de Predictions e também dados do Vehicle vinculado a Route.

   ```java
           public Flux<RouteResponse> getRoutesByAgencyTagWithPredictions(String agencyTag) {
                   return webClientConfig.getWebClient().get()
                                   .uri(uriBuilder -> uriBuilder
                                                   .queryParam("command", "routeConfig")
                                                   .queryParam("a", agencyTag)
                                                   .build())
                                   .retrieve().bodyToMono(String.class)
                                   .flatMap(xml -> Mono.fromCallable(() -> doXmlMapping(xml, RouteList.class))
                                                   .subscribeOn(Schedulers.boundedElastic()))
                                   .flatMapMany(routeList -> Flux
                                                   .fromIterable(routeList.routes() == null ? Collections.emptyList()
                                                                   : routeList.routes())
                                                   .subscribeOn(Schedulers.parallel())
                                                   .flatMap(route -> {
                                                           RouteResponse routeResponse = RouteMapper.fromRoute(route)
                                                                           .build();
                                                           Mono<RouteResponse> routeWithPredictions = enrichRouteWithPredictions(
                                                                           routeResponse,
                                                                           agencyTag);
                                                           return routeWithPredictions;
                                                   }, 8))
                                   .flatMap(routeResponse -> {
                                           Mono<RouteResponse> routeWithVehicle = getRouteVehicles(agencyTag,
                                                           routeResponse.tag())
                                                           .map(v -> {
                                                                   return RouteResponse.Builder.from(routeResponse)
                                                                                   .vehicles(v.vehicles())
                                                                                   .build();
                                                           });
                                           return routeWithVehicle;
                                   });
           }
   ```

7. **Desenhando as rotas para o usuário**: Agora, retornando a lista de rotas enriquecidas para o usuário, usamos o leaflet.js para desenhar: as rotas, suas paradas e os veículos no mapa. Como o objeto de cada Rota possui uma propriedade COLOR, usamos esta propriedade para definir que cor cada rota terá no mapa, facilitando a visualização pelo usuário.

   ```javascript
   function getAgencyRoutes(agencyTag) {
    const endpoint = `/api/bustrack/routes?agency=${agencyTag}`;
    let es = new EventSource(endpoint);

    es.onmessage = function (event) {
     const route = JSON.parse(event.data);
     console.log('Received data:', route);

     route.paths.forEach((path) => {
      const latlngs = path.points.map((p) => [p.lat, p.lon]);
      L.polyline(latlngs, { color: `#${route.color}`, dashArray: '5, 5' }).addTo(routesLayerGroup);
     }),
      route.stops.forEach((stop) => {
       let icon = L.icon({
        iconUrl: '../img/bus-stop.png',
        iconSize: [20, 20],
       });

       let busStopMarker = L.marker([stop.lat, stop.lon], { icon: icon }).addTo(routesLayerGroup);
       busStopMarker.bindPopup(`<b>${stop.title}</b><br>Stop ID: ${stop.stopId}`);
      });

     if (route.vehicles != null && route.vehicles.length > 0) {
      route.vehicles.forEach((v) => {
       let icon = L.icon({
        iconUrl: '../img/bus.png',
        iconSize: [70, 70],
       });

       vehicle = L.marker([v.lat, v.lon], { icon: icon }).addTo(routesLayerGroup);
       vehicle.bindPopup(
        `<b>Route: ${v.routeTag}</b><br>Vehicle ID: ${v.id}<br>Speed: ${v.speedKmHr} km/h<br>Heading: ${v.heading}°`
       );
      });
     }

     routeBound = new L.LatLngBounds([
      [route.latMax, route.lonMax],
      [route.latMin, route.lonMin],
     ]);
     //map.fitBounds(routeBound, { padding: [200, 200] });
    };
    return es;
   }
   ```

8. **Rastreando os dados em tempo-real**: Como a intenção da aplicação é exibir informações sobre as rotas, paradas e veículos em tempo-real, transformei o método de consulta da Rota em Server-Sent Events (SSE) endpoint. Isso pode ser feito usando o próprio Spring, definindo que tipo de dados o seu endpoint do controller retorna. É claro que para isso, dados devem estar sendo enviados (eventos sendo enviados) ativamente. Como a API NEXT BUS que é a API que consultamos para obter os dados das rotas não é uma API SSE nem WEBSOCKET, o que eu fiz foi: Usar o Flux.interval do Reactor para chamar o método de consulta a cada X segundos (criei uma váriavel `interval` para controlar esse tempo). Ou seja: a cada X (interval) segundos, um Flux de RouteResponse será retornado ao client, a configuração do client para receber esses eventos está explicada no próximo passo.

   ```java

   @Value("${routes.poll.interval:15}")
   private int interval;

   @GetMapping(path = "/routes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public Flux<RouteResponse> getRoutesByAgencyTag(
           @RequestParam(name = "agency") AgencyRoutesRequest agencyRoutesRequest)
           throws JsonMappingException, JsonProcessingException {

       return Flux.interval(Duration.ofSeconds(interval))
               .flatMap(tick -> busTrackService.getRoutesByAgencyTagWithPredictions(agencyRoutesRequest.agencyTag()));
   }
   ```

9. **Recebendo os eventos no client**: Para receber os eventos no client, usei a Interface EventSource. Essa interface é usada para receber eventos enviados por um servidor SSE, se conectando via HTTP ao servidor e recebendo os eventos no formato `text/event-stream` (como configuramos o endpoint do controller). A interface possui métodos como o `onmessage`, onde podemos definir O QUE deve ser feito quando um novo evento chegar, dentre outros métodos úteis.

   ```javascript
       const endpoint = `/api/bustrack/routes?agency=${agencyTag}`;
       let es = new EventSource(endpoint);

       es.onmessage = function (event) {
           ....
       }
   ```

**Mono e Flux**:

- Mono: Um Mono representa uma OPERAÇÃO ASSÍNCRONA que emite no máximo um único valor (ou nenhum). Ele é uma promessa que no futuro haverá: um valor do tipo T ou um erro ou nada.
- Flux: Um Flux representa uma OPERAÇÃO ASSÍNCRONA que pode emitir zero, um ou muitos valores. Representa um fluxo de dados contínuo ou finito. O Flux também pode sinalizar um erro ou conclusão, assim como o Mono.
- Em ambos podemos encadear transformações, utilizando métodos como flatMap, flatMapMany, reduce, map, filter etc. Esse é um dos objetivos do Reactor.

**Schedulers em programação reativa**:

- Em programação reativa no geral, um `Scheduler` controla em que thread (ou pool de threads) cada etapa do fluxo será executada. No Reactor, se nenhum scheduler for definido, todas as etapas do fluxo serão executadas na mesma thread que executor o fluxo.
- `Schedulers.parallel()`: Define que determinada etapa do fluxo será executada em threads de um pool paralelo (a documentação diz que usa um pool fixo de threads geralmente igual ao número de núcleos), otimizado para tarefas pesadas de CPU. Para operações complexas (como o nosso caso, onde precisamos de diferentes informações de diferentes endpoints, e alguns processamentos para estas informações) acaba sendo bem útil. No nosso caso, permite que várias listas (Routes) sejam tratadas simultaneamente.
- `Schedulers.boundedElastic()`: É usado para trabalhos bloqueantes ou demorados, como I/O, chamadas a API externas, conversão pesada de dados etc. Ele cria um pool elástico de threads, ou seja, cresce sob demanda, mas com um limite. Cada tarefa roda em uma thread separada e tem um limite máximo de thread (o padrão é 10 vezes o número de CPUs). Ideal e seguro para operações de I/O bloqueante. No nosso caso usamos sempre que usamos nosso método de conversão de XML para um objeto de classe, já que essa conversão PODE ser pesada e é não-reativa (bloqueante).

**FlatMap e FlatMapMany**:

- `FlatMap`: Serve para transformar cada elemento emitido em outro Publisher (Mono ou Flux) e depois os achata todos os resultados em um único fluxo. É útil quando é preciso fazer chamadas assíncronas ou operações reativas dentro de outra operação reativa. Ex: buscar dados de outro serviço (como fazemos requisitando a API NEXT BUS), salvar no db, etc.
- `FlatMapMany`: Transforma um Mono em Flux, quando o Mono emite uma coleção ou lista.
