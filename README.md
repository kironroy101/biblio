[![ATown Data](/resources/public/img/banner.png)](https://www.atowndata.com)

Biblio is a reference [AWS Cognito](https://aws.amazon.com/cognito/) ClojureScript/Reagent client. It supports the following flows:
* User registration
* User sign-in and sign-out
* Password recovery
* Account (email) confirmation

## Why Amazon Cognito?

Creating user management infrastructure is time consuming, with many edge cases and security pitfalls. AWS Cognito provides a low-cost solution that:
1. Supports common user flows
1. Is highly-available
1. Is secured
1. Is HIPAA-compliant

AWS Cognito supports JWT tokens and can secure API endpoints with JWT-based authentication.

For the above reasons, it is the preferred user management solution for us at [ATown Data](https://www.atowndata.com).

![ATown Data](/resources/public/img/screen.png)

## Usage

### Set AWS Cognito Pool

To change the AWS Cognito Pool, see `src/bib/customer/client/store.cljs` and change the `app-state > cognito` object to reflect the PoolID, ClientID, and Region for the Cognito UserPool and Client associated with the app.

### Launch the Project

Run the following commands from the root of the project: 

```
lein figwheel
```

The project will build and a browser window will launch showing the sign-in page.

```
lein auto sassc once
```

---

## Contacting Us / Contributions

Please use the [Github Issues](https://github.com/atowndata/trawler/issues) page for questions, ideas and bug reports. Pull requests are welcome.

Trawler was built by the consulting team at [ATown Data](https://www.atowndata.com). Please [contact us](https://atowndata.com/contact/) if you have a project you'd like to talk to us about!


## License

Distributed under the [Eclipse Public License](https://www.eclipse.org/legal/epl-v10.html) (the same license as Clojure).
Copyright &copy; 2017 [ATown Data](https://www.atowndata.com)