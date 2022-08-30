# elasticsearch 설정
1. javacafe plugin 설치
   a. 터미널에서 아래 명령어 실행
    - elasticsearch-plugin install file:파일경로(javacafe-analyzer-8.3.2.zip)
      b. 동의어 설정
    - config > analysis 폴더 생성 및 synonyms.txt 파일 추가
# logstash 설정
1. config -> conf.d 폴더 생성
2. conf.d 파일 내 local-mssql-baygleitem.conf 생성
   a. 내용 슬랙 참조
   b. logstash modules 추가
   - mssql-jdbc-8.2.0.jre8.jar
3. config -> pipelines.yml 파일 수정
   a. 아래 내용 추가
   b.  - pipeline.id: baygleitem
   path.config: D:\my_project\elasticsearch\logstash-8.3.2\config\conf.d\local-mssql-baygleitem.conf
# kibana 설정
1. setting, mapping 참조