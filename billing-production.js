(function(){
  if(window.__stackupBillingBound)return;
  window.__stackupBillingBound=true;

  window.StackUpBilling={
    onEntitlement:function(plan,active,status){
      try{
        var selected=active?(plan||"mensal"):"free";
        localStorage.setItem("academy.plan.v1",JSON.stringify({
          id:selected,
          since:Date.now(),
          source:"google_play",
          status:status||""
        }));
        if(typeof renderView==="function")renderView();
      }catch(_){}
    },
    onMessage:function(message){
      try{
        if(typeof toast==="function")toast(message,3500);
      }catch(_){}
    }
  };

  document.addEventListener("click",function(event){
    var button=event.target.closest&&event.target.closest("[data-buy]");
    if(!button)return;
    if(!(window.StackUpNative&&typeof window.StackUpNative.requestSubscription==="function"))return;
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
    window.StackUpNative.requestSubscription(String(button.dataset.buy||""));
  },true);

  try{
    if(typeof LX!=="undefined"){
      if(LX.pt)LX.pt.buySoon="Abrindo a assinatura na Google Play…";
      if(LX.en)LX.en.buySoon="Opening the subscription in Google Play…";
      if(LX.es)LX.es.buySoon="Abriendo la suscripción en Google Play…";
    }
  }catch(_){}

  if(window.StackUpNative&&typeof window.StackUpNative.restoreSubscriptions==="function"){
    window.StackUpNative.restoreSubscriptions();
  }
})();