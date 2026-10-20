The following assumption about the task and the service has been made:

- Do not allow overlap between slots
- Security and user stuff accepted as is by "userId"(mostly to simplify the data model) which in reality usually a completely different service and this service
  makes a call or included in jwt
- Meetings includes names/emails and not relay on user
- No caching or "reservation" in redis