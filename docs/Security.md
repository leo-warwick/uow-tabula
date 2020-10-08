Application security
====================

This document outlines some security information that is useful to have in mind when working on Tabula.

Permission checks
-----------------

These happen at command binding time.

It is not sufficient to manually call `apply()` on the command object.

CSRF
----

Tabula requires form-based CSRF tokens, along with `SameSite` session cookies as a defence in depth strategy.

Enforcement mode for form-based protection can be toggled from the sysadmin section of the application.

We also get some mitigation for free from the use of UUID identifiers for most entities.

Many forms originally used Spring `FormTag`s - we have a `CsrfEnrichedFormTag` configured to transparently provide a CSRF token
hidden input field.

If necessary, there is a macro which can be used to manually insert the hidden input field:

```ftl
<@csrf_macros.csrfHiddenInputField />
```

Finally, the token is exposed via a `<meta>` element so that [js-serverpipe](https://github.com/UniversityOfWarwick/js-serverpipe)
can access it.

XSS
---

Primarily mitigated by the `<#escape x as x?html>` FreeMarker directive. Presence enforced via static analysis.

As a defence in depth strategy, Tabula is migrating towards `.ftlh` templates, which perform auto-escaping for HTML out of the box.
If you modify a template which still uses the legacy `.ftl` extension, please consider spending a few minutes migrating it.

Finally, we are working towards a strict `Content-Security-Policy`. We already have a nonce-based strict policy in reporting mode,
you'll see templates include the `nonce` like so:

```ftl
<script type="text/javascript" nonce="${nonce()}">
```

If you're adding new scripts, and can't use an external JS file, please do include the `nonce` attribute.

Security headers
----------------

We set most of the other security headers you'd expect to see (X-Content-Type-Options, HSTS, Referrer-Policy, X-Frame-Options, X-XSS-Protection - in blocking mode, Feature-Policy, Expect-CT etc).

User uploads are served `Content-Disposition: attachment`.

Virus scanning
--------------

Uploads are passed through a ClamAV-backed microservice to scan for viruses.
