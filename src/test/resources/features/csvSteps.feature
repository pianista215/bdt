Feature: Test for CSV matcher

  Scenario: DataTable are equals CSV file
     When I read info from csv file 'src/test/resources/schemas/csv_1.csv'
     Then There are results found with:
       |id  | nombre  | apellido|
       |0   |Hugo     |Dominguez|
       |1   |Antonio  |Lopez|
       |2   |Pedro    |Bedia|
       |3   |Alvaro   |Lopez|
       |4   |Antonio  |Alfonso|
       |5   |Diego    |Martinez|




