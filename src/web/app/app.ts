import {Component, signal} from '@angular/core';
import {NavigationEnd, Router, RouterLink, RouterOutlet} from '@angular/router';
import {filter} from "rxjs";
import {Context} from "./model";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected context = signal<Context>(Context.Primaerfaelle);

  constructor(readonly router: Router) {
    this.router.events
        .pipe(
            filter(event => event instanceof NavigationEnd)
        )
        .subscribe((event: NavigationEnd) => {
          if (event.urlAfterRedirects.startsWith('/prime')) {
            this.context.set(Context.Primaerfaelle);
          } else if (event.urlAfterRedirects.startsWith('/all')) {
            this.context.set(Context.AlleFaelle);
          } else if (event.urlAfterRedirects.startsWith('/mv')) {
            this.context.set(Context.Modellvorhaben);
          } else {
            this.context.set(Context.Primaerfaelle);
          }
        });
  }

  protected readonly Context = Context;
}
