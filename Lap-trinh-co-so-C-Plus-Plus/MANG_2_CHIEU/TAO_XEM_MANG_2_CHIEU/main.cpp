#include <iostream>
#define size 20
/* run this program using the console pauser or add your own getch, system("pause") or input loop */
int n , m ;
int M[size][size];//khai bao mang 2 chieu co toi da 20 hang va 20 cot 
//==================//
int Input(int &n , char c){
	int code = 0 ;
	do{
		printf(" nhap n : "); code = scanf("%d",&n); fflush(stdin);
	}while(code == 0 || n<=0 || n > size);
}

//========================//
void TaoMang(int M[][size] , int n , int m  ){
	for(int i = 0 ; i<n;i++)
		for(int j = 0 ; j<m;j++){
		printf("Nhap M[%d][%d] =",i,j);
		scanf("%d",&M[i][j]);	
		}
}	
//========================//
void XuatMang(int M[][size] , int n , int m   ){
	for(int i = 0 ; i<n;i++){
		for(int j = 0 ; j<m;j++)
		printf("%d",M[i][j]);
		printf("\n ");
	}
	printf("\n ");
}

int Sochan(int M[][size] , int n , int m ){
	
}


int main(int argc, char** argv) {
  Input(n, 'n' );
  Input(m,'m');	
	TaoMang(M,n,m);	
	XuatMang(M,n,m);
	return 0;
}
