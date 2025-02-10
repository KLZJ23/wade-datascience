url = 'https://raw.githubusercontent.com/coreyjwade/bca_datasets/refs/heads/master/UCB_Admissions/historical_tuition.csv'
import pandas as pd
pd.set_option('display.max_rows', None)

df = pd.read_csv(url)

#df_all_constant = df[df.tuition_type=="All Constant"]
for i in range(6):
  x = df.tuition_type[i]
  x_no_spaces=x.replace(" ","_")
  globals()['df_%s' % x_no_spaces] = df[df.tuition_type==x]

#df_all_constant.head(45)
#df_2_Year_Constant.head(45)
#df.head(100)
#df.tail(20)

'''
for x in range(0, 9):
  globals()['string%s' % x] = 'Hello'
print(string3)
'''

#df_all_constant.describe()

#df.tail(20)

#df.info()

#df_4_Year_Constant.info()

#df_4_Year_Current.describe()

# 20 random
#df_2_Year_Constant.sample(20)

#df_4_Year_Constant.value_counts(df_4_Year_Constant['year'])

#df.loc[df['year'] == '2014-15', 'type':'year']