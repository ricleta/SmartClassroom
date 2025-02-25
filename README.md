# INF1304-Trabalho1
Projeto para INF1304 usando ContextNet para um sistema de monitorar presença de alunos em aula

# Como usar
> **🚧Aviso🚧**  
> Sugerimos usar o codespace ao invés de rodar localmente

## Subindo os containers
1. Navegue até o diretório de ```/scripts```:
    ```sh
    cd scripts/
    ```
2. Rode ```compile-all.sh``` para ter certeza que os jars estão corretos:
    ```sh
    ./compile-all.sh
    ```
3. Rode ```./start.sh``` para subir os containers.
    ```sh
    ./start.sh
    ```

## Subindo os usuários
1. Abra uma nova aba do terminal para o usuário
2. Navegue até o diretório de ```/scripts```:
    ```sh
    cd scripts/
    ```
3. Rode ```compile-mn.sh```, esse script também já move os jars para cada pasta de usuário
    ```sh
    ./compile-mn.sh
    ```
4. Navegue até o diretório do usuário X que quer executar
    saindo de ```/scripts```
    ```sh
    cd ..
    cd /mobile-node/UserX
    ```
5. Dentro do diretório do usuário rode:
    ```sh
    java -jar my-mn.jar
    ```

### O que o usuário pode fazer?
1. Se digitar T você pode trocar a localização do usuário digitando uma das abaixo
    - Localizações disponíveis:  T01, LABGRAD, L420, L522
2. Se digitar R você inicia o registro de presença para uma turma na data que desejar
    - O registro em si é guardado em ```/data/presence_table.csv```
    - Disciplinas disponíveis: inf1304, inf1748 
    - Turmas disponíveis: 
        - Para inf1304: 3WA
        - Para inf1748: 3WA, 3WB
3. Se digitar Z a execução do usuário termina

## Logs 
- ```/data/attendance_log.csv``` tem os registros de presença automáticos feitos a cada minuto
- ```/data/groups_log.csv``` tem os registros dos grupos de presença ou falta obtidos para cada usuário cada minuto
- ```/data/presence_table.csv``` tem os registros feitos à pedido de um usuário que escolheu a opção R

# Membros
- Bernardo Luiz Bach 1613231
- Francisco Meirelles Fleury 2210641
- Ricardo Bastos Leta Vieira 2110526
