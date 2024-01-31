package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=91e8fe16";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private List<Serie> series = new ArrayList<>();
    private SerieRepository repositorio;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                ================================
                
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar séries por Titulo
                5 - Buscar séries por Ator
                6 - Top 5 series
                7 - Buscar séries por categoria
                8 - Buscar por máximo de temporada e avalição mínima
                
                0 - Sair           
                
                ================================                      
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    vuscaTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    buscarPorMaxTempEMinAval();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }
    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
//        DadosSerie dadosSerie = getDadosSerie();
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()) {
            var serieEcontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEcontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEcontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEcontrada.setEpisodios(episodios);
            repositorio.save(serieEcontrada);
        } else {
            System.out.println("Serie não encontrada.");
        }
    }
    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("Dados da série: " + serieBuscada.get());
        } else {
            System.out.println("Serie não encontrada!");
        }
    }
    private void buscarSeriePorAtor() {
        System.out.println("Nome do ator: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Criterio de avaliacao de 1 a 10: ");
        var avaliacaoSerie = leitura.nextDouble();
        List<Serie> seriesEcontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacaoSerie);
        seriesEcontradas.forEach(s -> System.out.println("Serie: " + s.getTitulo() + " | Avaliação: " + s.getAvaliacao()));
    }
    private void vuscaTop5Series() {
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println("Serie: " + s.getTitulo() + " | Avaliação: " + s.getAvaliacao()));
    }
    private void buscarSeriePorCategoria() {
        System.out.println("Categoria que proucura: ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriePorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Categoria: " + nomeGenero);
        seriePorCategoria.forEach(s -> System.out.println("Serie: " + s.getTitulo() + " | Categoria: " + s.getGenero()));
    }
    private void buscarPorMaxTempEMinAval() {
        System.out.println("Máximo de temporadas: ");
        var maxTemp = leitura.nextInt();
        System.out.println("Avaliação mínima: ");
        var minAval = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findTituloByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(maxTemp, minAval);
        seriesEncontradas.forEach(s -> System.out.println("Serie: " + s.getTitulo() + " | TotalTemporada: " + s.getTotalTemporadas() + " | Avaliacao: " + s.getAvaliacao()));
    }
}