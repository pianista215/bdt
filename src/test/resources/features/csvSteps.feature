Feature: Test for CSV matcher

  Scenario: DataTable are equals CSV file
    When I read info from csv file 'src/test/resources/schemas/csv_1.csv' with separator ','
    Then There are results found with:
      | id | nombre  | apellido  |
      | 0  | Hugo    | Dominguez |
      | 1  | Antonio | Lopez     |
      | 2  | Pedro   | Bedia     |
      | 3  | Alvaro  | Lopez     |
      | 4  | Antonio | Alfonso   |
      | 5  | Diego   | Martinez  |

  Scenario: DataTable are equals CSV file with regex
    When I read info from csv file 'src/test/resources/schemas/csv_2.csv' with separator ','
    Then There are results found with:
      | id | uuid       | date                                    |
      | 0  | regex-uuid | regex-timestamp_yyyy-MM-dd HH:mm:ss.SSS |

  Scenario: DataTable are equals CSV file
    When I read info from csv file 'src/test/resources/schemas/csv_3.csv' with separator '\t'
    Then There are results found with:
      | id | nombre  | apellido  |
      | 0  | Hugo    | Dominguez |
      | 1  | Antonio | Lopez     |
      | 2  | Pedro   | Bedia     |
      | 3  | Alvaro  | Lopez     |
      | 4  | Antonio | Alfonso   |
      | 5  | Diego   | Martinez  |
