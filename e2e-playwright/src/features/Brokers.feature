Feature: Brokers page

  Scenario: Brokers visibility BrokerDetails visibility
      Given Brokers is visible
      When click on Brokers link
      Then Brokers heading visible
      Then the end of current URL should be "brokers"
      Given Brokers Uptime visible is: "true"
      Given Brokers Partitions visible is: "true"
      When Brokers cell element "1" clicked
      Given BrokerDetails name is: "BrokersBroker" header visible is: "true"
      Given BrokerDetails Log directories visible is: "true"
      Given BrokerDetails Configs visible is: "true"
      Given BrokerDetails Metrics visible is: "true"

  Scenario: Brokers visibility BrokerDetails visibility
      Given Brokers is visible
      When click on Brokers link
      Then Brokers heading visible
      Then the end of current URL should be "brokers"
      Given Brokers Uptime visible is: "true"
      Given Brokers Partitions visible is: "true"
      When Brokers cell element "1" clicked
      Given BrokerDetails name is: "BrokersBroker" header visible is: "true"
      When BrokerDetails Configs clicked
      Given BrokerDetails Configs Key visible is: "true"
      Given BrokerDetails Configs Value visible is: "true"
      Given BrokerDetails Configs Source visible is: "true"
      Then BrokerDetails searchfield visible is: "true"
      When BrokerDetails searchfield input is: "process.roles" cell value is: "broker,controller"
      When BrokerDetails searchfield input is: "broker,controller" cell value is: "process.roles"