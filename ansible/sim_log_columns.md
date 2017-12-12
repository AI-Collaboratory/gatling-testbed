RUN
  simulationClassName
  userDefinedSimulationId
  defaultSimulationId
  start (epoch)
  description (none currently, put git commit here?)
  2.0

USER
  scenario (login, for example)
  userId  (int)
  event.name (START/END)
  startDate (epoch)
  timestamp (epoch)

GROUP
  scenario
  userId
  serializeGroups(groupHierarchy)
  startTimestamp
  endTimestamp
  cumulatedResponseTime
  status

REQUEST
  scenario
  userId
  serializeGroups(groupHierarchy)
  name
  startTimestamp
  endTimestamp
  status
  serializeMessage(message)
  serializeExtraInfo(extraInfo)

ASSERTION
  base64String

GROUP (Error)
  message
  date
